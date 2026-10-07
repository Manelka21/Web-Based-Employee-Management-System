import { h, icon } from "../../core/dom.js";
import { fmtDate, fmtDateTime, fmtMoney, fullName } from "../../core/format.js";
import { can, hasRole, ROLES } from "../../core/permissions.js";
import { isOn } from "../../core/features.js";
import { AttendanceApi, EmployeesApi, LeaveApi, PayrollApi, PerformanceApi, TrainingApi } from "../../services/api.js";
import { departmentLabel, lookups, positionLabel } from "../../services/lookups.js";
import { asyncSection } from "../../components/feedback.js";
import { button, iconButton } from "../../components/button.js";
import { dataTable } from "../../components/table.js";
import { tabbedView } from "../../components/tabs.js";
import { avatar, card, kv, page } from "../../components/page.js";
import { statusBadge } from "../../components/status.js";
import { attendanceColumns, byDateDesc, enrollmentColumns, leaveColumns, payrollColumns, reviewColumns } from "../../widgets/columns.js";
import {
  correctAttendanceDialog,
  deleteEmployee,
  deleteAttendance,
  deleteReview,
  employeeDialog,
  enrolDialog,
  enrollmentAction,
  payslipDialog,
  reviewDialog,
  salaryDialog,
} from "../../widgets/dialogs.js";
import { decideLeave } from "../../widgets/selfService.js";

export default async function EmployeeDetailPage(ctx) {
  const { user } = ctx;
  const id = Number(ctx.params.id);
  const [employee, maps] = await Promise.all([EmployeesApi.get(id), lookups.maps()]);
  const name = fullName(employee);
  const isSelf = user.employeeId === id;
  const isHr = can(user, "manageEmployees");

  const headActions = isHr
    ? h(
        "div",
        { class: "row row--wrap" },
        button({ label: "Edit", icon: "edit", variant: "tonal", onClick: async () => (await employeeDialog(employee)) && ctx.reload() }),
        !isSelf && button({ label: "Delete", icon: "delete", variant: "danger-text", onClick: async () => (await deleteEmployee(employee)) && ctx.navigate("/employees") })
      )
    : null;

  const head = h(
    "section",
    { class: "profile-head" },
    avatar(name, { size: "xl" }),
    h(
      "div",
      { class: "profile-head__body" },
      h("h1", { class: "headline-medium" }, name),
      h("p", { class: "body-large muted" }, `${positionLabel(maps.positions, employee.positionId)} · ${departmentLabel(maps.departments, employee.departmentId)}`),
      h("div", { class: "row row--wrap", style: { marginTop: "10px", "--gap": "8px" } }, statusBadge(employee.status), h("span", { class: "body-small muted" }, `Employee #${employee.employeeId}`))
    ),
    headActions
  );

  const tableSection = (loader, build) => asyncSection(loader, build);

  // Salary on file: only HR and Payroll see pay, and nobody sees/sets their own here
  const salaryCard = () =>
    can(user, "manageSalaries") && !isSelf && isOn("payroll")
      ? asyncSection(
          () => PayrollApi.salary(id).catch((err) => (err.status === 404 ? null : Promise.reject(err))),
          (salary, reload) =>
            card(
              {
                title: "Salary",
                subtitle: salary ? `Last changed ${fmtDateTime(salary.updatedAt)}` : "Used as the default when payroll is generated",
                actions:
                  employee.status !== "INACTIVE" &&
                  button({ label: salary ? "Change" : "Set salary", icon: "edit", variant: "tonal", size: "sm", onClick: async () => (await salaryDialog(employee, salary)) && reload() }),
              },
              salary ? h("div", { class: "headline-small num" }, fmtMoney(salary.baseSalary), h("span", { class: "body-medium muted" }, " / month")) : h("p", { class: "body-medium muted" }, "No salary on file yet.")
            )
        )
      : null;

  const tabs = tabbedView({
    active: ctx.query.tab,
    items: [
      { id: "overview", label: "Overview", icon: "badge" },
      { id: "attendance", label: "Attendance", icon: "schedule", visible: isOn("attendance") },
      { id: "leave", label: "Leave", icon: "beach_access", visible: can(user, "viewEmployeeLeave") && isOn("leave") },
      { id: "payroll", label: "Payroll", icon: "payments", visible: (can(user, "viewAllPayroll") || isSelf) && isOn("payroll") },
      { id: "performance", label: "Performance", icon: "trending_up", visible: can(user, "viewPerformance") && isOn("performance") },
      { id: "training", label: "Training", icon: "school" },
    ],
    render: (tab) => {
      switch (tab) {
        case "attendance":
          return tableSection(
            async () => (await AttendanceApi.byEmployee(id)).sort(byDateDesc("eventDate")),
            (rows, reload) =>
              dataTable({
                rows,
                searchable: false,
                filters: [
                  { id: "all", label: "All" },
                  { id: "LATE", label: "Late", predicate: (r) => r.attendanceStatus === "LATE" },
                  { id: "ABSENT", label: "Absent", predicate: (r) => r.attendanceStatus === "ABSENT" },
                ],
                columns: attendanceColumns({
                  actions: can(user, "logAttendance")
                    ? (r) => [
                        iconButton({ icon: "edit_calendar", label: "Correct", size: "sm", onClick: async () => (await correctAttendanceDialog(r, name)) && reload() }),
                        can(user, "deleteAttendance") && iconButton({ icon: "delete", label: "Delete", size: "sm", onClick: async () => (await deleteAttendance(r)) && reload() }),
                      ]
                    : null,
                }),
                empty: { icon: "event_busy", title: "No attendance records" },
              })
          );

        case "leave":
          return tableSection(
            async () => (await LeaveApi.byEmployee(id)).sort(byDateDesc("startDate")),
            (rows, reload) =>
              dataTable({
                rows,
                searchable: false,
                columns: leaveColumns({
                  actions:
                    can(user, "approveLeave") && !isSelf
                      ? (r) =>
                          r.leaveStatus === "PENDING" && [
                            iconButton({ icon: "close", label: "Reject", size: "sm", onClick: async () => (await decideLeave(r, "reject")) && reload() }),
                            iconButton({ icon: "check", label: "Approve", size: "sm", variant: "tonal", onClick: async () => (await decideLeave(r, "approve")) && reload() }),
                          ]
                      : null,
                }),
                empty: { icon: "beach_access", title: "No leave requests" },
              })
          );

        case "payroll":
          return tableSection(
            async () => (await PayrollApi.byEmployee(id)).sort(byDateDesc("payPeriodEnd")),
            (rows) =>
              dataTable({
                rows,
                searchable: false,
                columns: payrollColumns({}),
                onRowClick: (r) => payslipDialog(r, name),
                empty: { icon: "receipt_long", title: "No payroll records" },
              })
          );

        case "performance":
          return tableSection(
            async () => (await PerformanceApi.byEmployee(id)).sort(byDateDesc("reviewDate")),
            (rows, reload) =>
              dataTable({
                rows,
                searchable: false,
                headerActions: can(user, "recordPerformance") && !isSelf ? button({ label: "Record review", icon: "rate_review", variant: "tonal", size: "sm", onClick: async () => (await reviewDialog({ employeeId: id, selfEmployeeId: user.employeeId })) && reload() }) : null,
                title: "Reviews",
                columns: reviewColumns({
                  actions: can(user, "deletePerformance")
                    ? (r) => [
                        hasRole(user, ROLES.SUPERVISOR) && r.supervisorId === user.userId && iconButton({ icon: "edit", label: "Edit", size: "sm", onClick: async () => (await reviewDialog({ existing: r })) && reload() }),
                        (hasRole(user, ROLES.HR) || r.supervisorId === user.userId) && iconButton({ icon: "delete", label: "Delete", size: "sm", onClick: async () => (await deleteReview(r)) && reload() }),
                      ]
                    : null,
                }),
                empty: { icon: "rate_review", title: "No reviews yet" },
              })
          );

        case "training":
          return tableSection(
            async () => {
              const [enrollments, programs] = await Promise.all([TrainingApi.byEmployee(id), TrainingApi.programs()]);
              return { enrollments: enrollments.sort(byDateDesc("enrolledDate")), programs: new Map(programs.map((p) => [p.programId, p])) };
            },
            ({ enrollments, programs }, reload) =>
              dataTable({
                rows: enrollments,
                searchable: false,
                title: "Enrollments",
                headerActions: can(user, "enrolOthers") ? button({ label: "Enrol", icon: "add", variant: "tonal", size: "sm", onClick: async () => (await enrolDialog({ employeeId: id })) && reload() }) : null,
                columns: enrollmentColumns({
                  programs,
                  actions: can(user, "manageEnrollments")
                    ? (r) =>
                        ["ENROLLED", "IN_PROGRESS"].includes(r.completionStatus) && [
                          iconButton({ icon: "task_alt", label: "Mark completed", size: "sm", onClick: async () => (await enrollmentAction(r, "complete")) && reload() }),
                          iconButton({ icon: "remove_circle", label: "Drop", size: "sm", onClick: async () => (await enrollmentAction(r, "drop")) && reload() }),
                        ]
                    : null,
                }),
                empty: { icon: "school", title: "Not enrolled in any training" },
              })
          );

        default:
          return h(
            "div",
            { class: "stack", style: { "--gap": "16px" } },
            card(
            { title: "Details" },
            kv([
              ["Work email", h("a", { href: `mailto:${employee.email}` }, employee.email)],
              ["Phone", employee.phone || "—"],
              ["NIC", employee.nic],
              ["Department", departmentLabel(maps.departments, employee.departmentId)],
              ["Position", positionLabel(maps.positions, employee.positionId)],
              ["Hire date", fmtDate(employee.hireDate)],
              ["Status", statusBadge(employee.status)],
              ["Gender", employee.gender ? employee.gender.charAt(0) + employee.gender.slice(1).toLowerCase() : "Not recorded"],
              ["Record created", fmtDateTime(employee.createdAt)],
              ["Address", employee.address || "—"],
            ])
            ),
            salaryCard()
          );
      }
    },
  });

  return page(
    can(user, "listEmployees") &&
      h("div", { class: "page-header__eyebrow", style: { marginBottom: "12px" } }, h("a", { href: "#/employees" }, "Directory"), icon("chevron_right", { size: "xs" }), h("span", null, name)),
    h("div", { class: "stack", style: { "--gap": "24px" } }, head, tabs)
  );
}
