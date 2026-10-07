// Home — a role-aware overview. Each block loads independently so one failing
// endpoint never blanks the page.

import { h } from "../core/dom.js";
import { fmtDateTime, fmtMoney, fmtNumber, fmtRange, fullName, greeting, fmtWeekday, humanize, monthBounds, todayISO } from "../core/format.js";
import { can, hasRole, isLinked, ROLE_LABELS, ROLES } from "../core/permissions.js";
import { ActivityApi, AttendanceApi, DashboardApi, NotificationsApi, PayrollApi, ReportsApi, UsersApi } from "../services/api.js";
import { employeeLabel, lookups } from "../services/lookups.js";
import { asyncSection, emptyState, skeleton } from "../components/feedback.js";
import { banner, barList, card, page, personCell, stat } from "../components/page.js";
import { button } from "../components/button.js";
import { statusBadge } from "../components/status.js";
import { attendanceTodayCard, latestPayslipCard, leaveBalanceCard, pendingApprovalsCard } from "../widgets/selfService.js";
import { notificationItem } from "../widgets/notifications.js";
import { openAccountRequests } from "../widgets/accounts.js";
import { hrefOn, isOn } from "../core/features.js";

const statsPlaceholder = (n = 4) => () =>
  h("div", { class: `grid grid--${n}` }, Array.from({ length: n }, () => h("div", { class: "skeleton", style: { height: "128px", borderRadius: "16px" } })));

const section = (title, ...children) => h("section", { class: "section" }, title && h("h2", { class: "section__title" }, title), children);

export default function HomePage(ctx) {
  const { user } = ctx;
  const firstName = (user.fullName || "").split(" ")[0];

  const view = page(
    h(
      "section",
      { class: "hero" },
      h(
        "div",
        { style: { flex: "1 1 320px" } },
        h("h1", { class: "hero__title" }, `${greeting()}, ${firstName}`),
        h("p", { class: "hero__sub" }, `${fmtWeekday()} · ${ROLE_LABELS[user.role] || user.role}`)
      )
    )
  );

  // Self-service cards, minus any whose function is switched off
  const selfService = [isOn("attendance") && attendanceTodayCard(ctx), isOn("leave") && leaveBalanceCard(), isOn("payroll") && latestPayslipCard()].filter(Boolean);
  if (isLinked(user)) {
    if (selfService.length) view.append(section(null, h("div", { class: `grid grid--${Math.max(selfService.length, 2)}` }, selfService)));
  } else if (hasRole(user, ROLES.EMPLOYEE)) {
    view.append(
      banner("info", "link", h("strong", null, "Your account isn't linked to an employee record yet. "), "Ask your IT administrator to link it to use check-in, leave and payslips.")
    );
  }

  if (can(user, "viewCompanyDashboard")) view.append(companyOverview(user));
  if (hasRole(user, ROLES.SUPERVISOR) && (isOn("employees") || isOn("attendance") || isOn("leave"))) view.append(teamOverview());
  if (hasRole(user, ROLES.PAYROLL) && isOn("payroll")) view.append(payrollOverview());
  if (hasRole(user, ROLES.ADMIN) && (isOn("userAdmin") || isOn("activityLog"))) view.append(adminOverview());

  view.append(recentNotifications());
  return view;
}

// ---------------------------------------------------------------- Director / HR
function companyOverview(user) {
  const isHr = hasRole(user, ROLES.HR);
  return section(
    "Company at a glance",
    asyncSection(
      () => DashboardApi.summary(),
      (s) => {
        const stats = [
          stat({ label: "Employees", icon: "group", value: fmtNumber(s.totalEmployees), meta: `${s.activeEmployees} active · ${s.employeesOnProbation} on probation`, href: hrefOn("#/employees"), accent: true }),
          isOn("leave")
            ? stat({ label: "Pending leave", icon: "pending_actions", value: fmtNumber(s.pendingLeaveRequests), meta: `${s.employeesOnLeave} on leave now`, href: hrefOn(isHr ? "#/leave?tab=approvals" : "#/reports?tab=leave") })
            : stat({ label: "Departments", icon: "apartment", value: fmtNumber(s.departments), meta: "Departments and their positions", href: "#/organization" }),
          stat({ label: "Open vacancies", icon: "work", value: fmtNumber(s.activeVacancies), meta: `${s.newApplications} new applications`, href: isHr ? "#/recruitment" : hrefOn("#/reports?tab=recruitment") }),
          isOn("attendance") &&
            stat({ label: "Checked in today", icon: "how_to_reg", value: fmtNumber(s.todayAttendance), meta: [`${s.departments} departments`, isOn("payroll") && `${s.draftPayrolls} draft payrolls`].filter(Boolean).join(" · ") }),
        ].filter(Boolean);
        return h("div", { class: `grid grid--${stats.length}` }, stats);
      },
      { placeholder: statsPlaceholder(4) }
    ),
    h(
      "div",
      { class: "grid grid--2", style: { marginTop: "16px" } },
      card(
        { title: "Headcount by department", actions: isOn("reports") ? button({ label: "Reports", variant: "text", size: "sm", href: "#/reports" }) : null },
        asyncSection(
          () => ReportsApi.headcount(),
          (rows) =>
            rows.length
              ? barList(rows.map((r) => ({ label: r.departmentName, value: Number(r.totalEmployees) || 0 })))
              : emptyState({ icon: "apartment", title: "No departments yet" })
        )
      ),
      isHr && isOn("leave")
        ? pendingApprovalsCard()
        : card(
            { title: "Recruitment pipeline", flush: true },
            asyncSection(
              () => ReportsApi.recruitment(),
              (rows) =>
                rows.length
                  ? h(
                      "div",
                      { class: "list", style: { paddingBottom: "8px" } },
                      rows.slice(0, 5).map((r) =>
                        h(
                          "div",
                          { class: "list-item" },
                          h("div", { class: "list-item__body" }, h("div", { class: "list-item__title" }, r.title), h("div", { class: "list-item__text" }, `${r.applications} applications · ${r.shortlisted ?? 0} shortlisted · ${r.hired ?? 0} hired`)),
                          statusBadge(r.status)
                        )
                      )
                    )
                  : emptyState({ icon: "work_off", title: "No vacancies posted" }),
              { placeholder: () => h("div", { style: { padding: "0 24px 16px" } }, skeleton({ rows: 3 })) }
            )
          )
    )
  );
}

// ---------------------------------------------------------------- Supervisor
function teamOverview() {
  return section(
    "Your team",
    asyncSection(
      async () => {
        const me = await lookups.myEmployee();
        const [team, attendance] = await Promise.all([lookups.team(), me?.departmentId && isOn("attendance") ? AttendanceApi.byDepartment(me.departmentId) : []]);
        return { team, attendance };
      },
      ({ team, attendance }) => {
        const today = attendance.filter((a) => a.eventDate === todayISO());
        const late = today.filter((a) => a.attendanceStatus === "LATE").length;
        return h(
          "div",
          { class: `grid grid--${Math.max(2, 1 + isOn("attendance") + isOn("leave"))}` },
          stat({ label: "Team members", icon: "groups", value: fmtNumber(team.length), meta: `${team.filter((e) => e.status === "ACTIVE").length} active`, href: hrefOn("#/employees"), accent: true }),
          isOn("attendance") && stat({ label: "Checked in today", icon: "how_to_reg", value: `${today.length}`, meta: late ? `${late} late` : "No late arrivals", href: "#/attendance?tab=team" }),
          isOn("leave") && stat({ label: "On leave", icon: "beach_access", value: fmtNumber(team.filter((e) => e.status === "ON_LEAVE").length), meta: `${team.filter((e) => e.status === "PROBATION").length} on probation` })
        );
      },
      { placeholder: statsPlaceholder(3) }
    ),
    isOn("leave") && h("div", { style: { marginTop: "16px" } }, pendingApprovalsCard())
  );
}

// ---------------------------------------------------------------- Payroll executive
function payrollOverview() {
  return section(
    "Payroll",
    asyncSection(
      async () => {
        const [records, maps] = await Promise.all([PayrollApi.list(), lookups.maps()]);
        return { records, maps };
      },
      ({ records, maps }) => {
        const { start, end } = monthBounds();
        const count = (status) => records.filter((r) => r.status === status).length;
        const paidThisMonth = records
          .filter((r) => r.status === "PAID" && r.payPeriodStart >= start && r.payPeriodEnd <= end)
          .reduce((sum, r) => sum + Number(r.netPay || 0), 0);
        const recent = [...records].sort((a, b) => String(b.generatedAt).localeCompare(String(a.generatedAt))).slice(0, 5);

        return h(
          "div",
          { class: "stack" },
          h(
            "div",
            { class: "grid grid--3" },
            stat({ label: "Drafts to finalize", icon: "edit_note", value: fmtNumber(count("DRAFT")), href: "#/payroll?tab=all", accent: true }),
            stat({ label: "Awaiting payment", icon: "hourglass_top", value: fmtNumber(count("FINALIZED")), href: "#/payroll?tab=all" }),
            stat({ label: "Paid this month", icon: "account_balance", value: fmtMoney(paidThisMonth), meta: `${count("PAID")} payslips paid in total` })
          ),
          card(
            { title: "Recently generated", flush: true, actions: button({ label: "Generate payroll", icon: "add", variant: "tonal", size: "sm", href: "#/payroll?tab=all&action=generate" }) },
            recent.length
              ? h(
                  "div",
                  { class: "list", style: { paddingBottom: "8px" } },
                  recent.map((r) =>
                    h(
                      "div",
                      { class: "list-item" },
                      h("div", { class: "list-item__body" }, personCell(employeeLabel(maps.employees, r.employeeId), fmtRange(r.payPeriodStart, r.payPeriodEnd))),
                      h("span", { class: "body-medium num" }, fmtMoney(r.netPay)),
                      statusBadge(r.status)
                    )
                  )
                )
              : emptyState({ icon: "request_quote", title: "No payroll records yet" })
          )
        );
      },
      { placeholder: statsPlaceholder(3) }
    )
  );
}

// ---------------------------------------------------------------- IT admin
function adminOverview() {
  return section(
    "System",
    h(
      "div",
      { class: isOn("userAdmin") && isOn("activityLog") ? "grid grid--2" : "stack" },
      isOn("userAdmin") && card(
        { title: "User accounts", actions: button({ label: "Manage", variant: "text", size: "sm", href: "#/admin/users" }) },
        asyncSection(
          async () => {
            const users = await UsersApi.list();
            return { users, requests: await openAccountRequests(users) };
          },
          ({ users, requests }) => {
            const unlinked = users.filter((u) => u.employeeId == null && ["EMPLOYEE", "DEPT_SUPERVISOR"].includes(u.role)).length;
            const locked = users.filter((u) => u.locked).length;
            const disabled = users.filter((u) => !u.active).length;
            const byRole = Object.entries(ROLE_LABELS).map(([role, label]) => ({ label, value: users.filter((u) => u.role === role).length }));
            return h(
              "div",
              { class: "stack", style: { "--gap": "20px" } },
              h(
                "div",
                { class: "grid grid--2" },
                stat({ label: "Accounts", icon: "badge", value: fmtNumber(users.length), meta: `${fmtNumber(unlinked)} need linking`, accent: true, href: "#/admin/users?filter=unlinked" }),
                stat({ label: "Password resets", icon: "lock_reset", value: fmtNumber(requests.resets.size), meta: "Forgot-password requests", href: "#/admin/users?filter=resets" }),
                stat({ label: "Access requests", icon: "how_to_reg", value: fmtNumber(requests.roles.size), meta: "Waiting for a role", href: "#/admin/users?filter=requests" }),
                stat({ label: "Locked or disabled", icon: "lock_person", value: fmtNumber(locked + disabled), meta: `${locked} locked · ${disabled} disabled`, href: `#/admin/users?filter=${locked ? "locked" : "disabled"}` })
              ),
              barList(byRole)
            );
          }
        )
      ),
      isOn("activityLog") && card(
        { title: "Recent activity", flush: true, actions: button({ label: "Full log", variant: "text", size: "sm", href: "#/admin/activity" }) },
        asyncSection(
          () => ActivityApi.recent(8),
          (logs) =>
            logs.length
              ? h(
                  "div",
                  { class: "list", style: { paddingBottom: "8px" } },
                  logs.map((log) =>
                    h(
                      "div",
                      { class: "list-item" },
                      h("div", { class: "list-item__body" }, h("div", { class: "list-item__title" }, humanize(log.action)), h("div", { class: "list-item__text truncate" }, log.details || `User #${log.userId}`)),
                      h("span", { class: "list-item__meta" }, fmtDateTime(log.timestamp))
                    )
                  )
                )
              : emptyState({ icon: "history", title: "No activity yet" }),
          { placeholder: () => h("div", { style: { padding: "0 24px 16px" } }, skeleton({ rows: 4 })) }
        )
      )
    )
  );
}

// ---------------------------------------------------------------- Everyone
function recentNotifications() {
  return section(
    null,
    card(
      { title: "Recent notifications", flush: true, actions: button({ label: "See all", variant: "text", size: "sm", href: "#/notifications" }) },
      asyncSection(
        () => NotificationsApi.list(),
        (items) =>
          items.length
            ? h("div", { class: "list", style: { paddingBottom: "8px" } }, items.slice(0, 5).map((n) => notificationItem(n)))
            : emptyState({ icon: "notifications_off", title: "You're all caught up", text: "Updates about your leave, payslips and training appear here." }),
        { placeholder: () => h("div", { style: { padding: "0 24px 16px" } }, skeleton({ rows: 3 })) }
      )
    )
  );
}

export { fullName };
