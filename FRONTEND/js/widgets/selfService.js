// Self-service cards shared by the home page and the feature pages.

import { h, icon } from "../core/dom.js";
import { fmtDate, fmtMoney, fmtRange, fmtTime, fmtWeekday, humanize, todayISO } from "../core/format.js";
import { AttendanceApi, LeaveApi, PayrollApi } from "../services/api.js";
import { employeeLabel, lookups } from "../services/lookups.js";
import { asyncSection, emptyState, skeleton } from "../components/feedback.js";
import { button, iconButton, withBusy } from "../components/button.js";
import { card, meter, personCell } from "../components/page.js";
import { statusBadge } from "../components/status.js";
import { toast, toastError } from "../components/snackbar.js";
import { confirmDialog, formDialog } from "../components/dialog.js";

// ---------------------------------------------------------------- Attendance today
export function attendanceTodayCard(ctx, { onChange } = {}) {
  const clock = h("div", { class: "clock", "aria-live": "off" });
  const tick = () => {
    clock.textContent = new Date().toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" });
  };
  tick();
  const timer = setInterval(tick, 10000);
  ctx.onCleanup(() => clearInterval(timer));

  const body = asyncSection(
    () => AttendanceApi.mine(),
    (records, reload) => {
      const today = records.find((r) => r.eventDate === todayISO());
      const act = (fn, message) => async (event) => {
        try {
          await withBusy(event.currentTarget, fn);
          toast(message);
          reload();
          onChange?.();
        } catch (err) {
          toastError(err);
        }
      };

      let line;
      let action;
      if (!today) {
        line = h("p", { class: "body-medium muted" }, "You haven't checked in yet. Check-ins after 09:00 count as late.");
        action = button({ label: "Check in", icon: "login", onClick: act(() => AttendanceApi.checkIn(), "Checked in. Have a good day.") });
      } else if (!today.checkOutTime && today.checkInTime) {
        line = h("div", { class: "row row--wrap", style: { "--gap": "8px" } }, h("span", { class: "body-medium" }, `Checked in at ${fmtTime(today.checkInTime)}`), statusBadge(today.attendanceStatus));
        action = button({ label: "Check out", icon: "logout", variant: "tonal", onClick: act(() => AttendanceApi.checkOut(), "Checked out. See you tomorrow.") });
      } else {
        line = h(
          "div",
          { class: "row row--wrap", style: { "--gap": "8px" } },
          h("span", { class: "body-medium" }, today.checkInTime ? `${fmtTime(today.checkInTime)} – ${fmtTime(today.checkOutTime)}` : "Recorded for today"),
          statusBadge(today.attendanceStatus)
        );
        action = h("span", { class: "row body-medium muted", style: { "--gap": "6px" } }, icon("check_circle", { size: "sm", fill: true }), "Done for today");
      }
      return h("div", { class: "stack", style: { "--gap": "16px" } }, line, h("div", null, action));
    },
    { placeholder: () => skeleton({ rows: 2 }) }
  );

  return card({ title: "Today", subtitle: fmtWeekday() }, h("div", { class: "stack", style: { "--gap": "12px" } }, clock, body));
}

// ---------------------------------------------------------------- Leave balance
const BALANCE_ORDER = ["ANNUAL", "CASUAL", "SICK", "MATERNITY", "OTHER"];

export function leaveBalanceList(rows) {
  const visible = rows
    .filter((r) => r.entitlement != null && (r.leaveType !== "MATERNITY" || r.approvedDays + r.pendingDays > 0))
    .sort((a, b) => BALANCE_ORDER.indexOf(a.leaveType) - BALANCE_ORDER.indexOf(b.leaveType));

  return h(
    "div",
    { class: "stack", style: { "--gap": "16px" } },
    visible.map((r) => {
      const used = r.approvedDays + r.pendingDays;
      return h(
        "div",
        { class: "stack", style: { "--gap": "6px" } },
        h(
          "div",
          { class: "row row--between" },
          h("span", { class: "body-medium" }, humanize(r.leaveType)),
          h("span", { class: "body-small muted num" }, `${r.remainingDays} of ${r.entitlement} days left`)
        ),
        meter(used, r.entitlement, { warn: r.remainingDays <= 2 }),
        r.pendingDays > 0 && h("span", { class: "body-small muted" }, `${r.pendingDays} day${r.pendingDays === 1 ? "" : "s"} pending approval`)
      );
    })
  );
}

export function leaveBalanceCard() {
  return card(
    { title: "Leave balance", subtitle: String(new Date().getFullYear()), actions: button({ label: "Apply", icon: "add", variant: "tonal", size: "sm", href: "#/leave?action=apply" }) },
    asyncSection(() => LeaveApi.balance(), leaveBalanceList, { placeholder: () => skeleton({ rows: 3 }) })
  );
}

// ---------------------------------------------------------------- Latest payslip
export function latestPayslipCard() {
  return card(
    { title: "Latest payslip", actions: button({ label: "All payslips", variant: "text", size: "sm", href: "#/payroll" }) },
    asyncSection(
      () => PayrollApi.mine(),
      (slips) => {
        const latest = [...slips].sort((a, b) => String(b.payPeriodEnd).localeCompare(String(a.payPeriodEnd)))[0];
        if (!latest) return emptyState({ icon: "receipt_long", title: "No payslips yet", text: "Payslips appear here once payroll is finalized." });
        return h(
          "div",
          { class: "stack", style: { "--gap": "8px" } },
          h("span", { class: "body-small muted" }, "Net pay"),
          h("span", { class: "headline-medium num" }, fmtMoney(latest.netPay)),
          h("div", { class: "row row--wrap", style: { "--gap": "8px" } }, h("span", { class: "body-medium muted" }, fmtRange(latest.payPeriodStart, latest.payPeriodEnd)), statusBadge(latest.status))
        );
      },
      { placeholder: () => skeleton({ rows: 3 }) }
    )
  );
}

// ---------------------------------------------------------------- Pending approvals
export async function decideLeave(request, decision) {
  const approve = decision === "approve";
  return formDialog({
    title: approve ? "Approve leave?" : "Reject leave?",
    intro: `${humanize(request.leaveType)} leave, ${fmtRange(request.startDate, request.endDate)} (${request.leaveDays} day${request.leaveDays === 1 ? "" : "s"}).`,
    fields: [{ name: "comment", label: approve ? "Comment (optional)" : "Reason (optional)", type: "textarea", rows: 3, maxLength: 300, span: "all" }],
    submitLabel: approve ? "Approve" : "Reject",
    successMessage: approve ? "Leave approved" : "Leave rejected",
    onSubmit: ({ comment }) => (approve ? LeaveApi.approve(request.eventId, comment) : LeaveApi.reject(request.eventId, comment)),
  });
}

export function pendingApprovalsCard({ limit = 5 } = {}) {
  return card(
    { title: "Waiting for your approval", flush: true, actions: button({ label: "Review all", variant: "text", size: "sm", href: "#/leave?tab=approvals" }) },
    asyncSection(
      async () => {
        const [pending, maps] = await Promise.all([LeaveApi.pending(), lookups.maps()]);
        return { pending, maps };
      },
      ({ pending, maps }, reload) => {
        if (!pending.length) return emptyState({ icon: "task_alt", title: "Nothing to review", text: "New leave requests from your team will show up here." });
        return h(
          "div",
          { class: "list", style: { paddingBottom: "8px" } },
          pending.slice(0, limit).map((req) =>
            h(
              "div",
              { class: "list-item" },
              h("div", { class: "list-item__body" }, personCell(employeeLabel(maps.employees, req.employeeId), `${humanize(req.leaveType)} · ${fmtRange(req.startDate, req.endDate)} · ${req.leaveDays} work day${req.leaveDays === 1 ? "" : "s"}`)),
              iconButton({ icon: "close", label: "Reject", onClick: async () => (await decideLeave(req, "reject")) && reload() }),
              iconButton({ icon: "check", label: "Approve", variant: "tonal", onClick: async () => (await decideLeave(req, "approve")) && reload() })
            )
          )
        );
      },
      { placeholder: () => h("div", { style: { padding: "0 24px 16px" } }, skeleton({ rows: 3 })) }
    )
  );
}

export async function cancelLeave(request) {
  const ok = await confirmDialog({
    title: "Cancel this leave request?",
    message: `${humanize(request.leaveType)} leave, ${fmtRange(request.startDate, request.endDate)}. Your supervisor will no longer see it.`,
    confirmLabel: "Cancel request",
    cancelLabel: "Keep",
  });
  if (!ok) return false;
  try {
    await LeaveApi.cancel(request.eventId);
    toast("Leave request cancelled");
    return true;
  } catch (err) {
    toastError(err);
    return false;
  }
}
