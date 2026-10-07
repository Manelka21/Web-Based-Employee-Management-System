// Company reports. Each tab is gated by the same roles as its endpoint.

import { h } from "../core/dom.js";
import { fmtDate, fmtMoney, fmtNumber, humanize, monthBounds } from "../core/format.js";
import { can } from "../core/permissions.js";
import { isOn } from "../core/features.js";
import { downloadCsv } from "../core/csv.js";
import { ReportsApi } from "../services/api.js";
import { departmentLabel, lookups } from "../services/lookups.js";
import { asyncSection, emptyState } from "../components/feedback.js";
import { button } from "../components/button.js";
import { compactInput, compactSelect } from "../components/fields.js";
import { dataTable } from "../components/table.js";
import { tabbedView } from "../components/tabs.js";
import { barList, card, meter, page, pageHeader, ratingStars, stat } from "../components/page.js";
import { statusBadge } from "../components/status.js";

// Both date filters must be set and in order before the API is called
function checkedRange(from, to) {
  const start = from.control.value;
  const end = to.control.value;
  if (!start || !end) throw Object.assign(new Error("Choose both a From and a To date."), { status: 400 });
  if (end < start) throw Object.assign(new Error("The To date can't be before the From date."), { status: 400 });
  return [start, end];
}

const csvButton = (filename, columns, rows) =>
  button({ label: "Export CSV", icon: "download", variant: "text", size: "sm", disabled: !rows.length, onClick: () => downloadCsv(filename, columns, rows) });

// ---------------------------------------------------------------- Headcount
const headcountView = () =>
  asyncSection(
    () => ReportsApi.headcount(),
    (rows) => {
      const total = rows.reduce((s, r) => s + Number(r.totalEmployees || 0), 0);
      const columns = [
        { label: "Department", value: (r) => r.departmentName },
        { label: "Total", value: (r) => r.totalEmployees },
        { label: "Active", value: (r) => r.activeEmployees },
        { label: "Probation", value: (r) => r.probationEmployees },
      ];
      return h(
        "div",
        { class: "stack" },
        h(
          "div",
          { class: "grid grid--2" },
          card({ title: "Employees by department", subtitle: `${fmtNumber(total)} in total` }, rows.length ? barList(rows.map((r) => ({ label: r.departmentName, value: Number(r.totalEmployees) || 0 }))) : emptyState({ icon: "apartment", title: "No departments" })),
          h(
            "div",
            { class: "grid grid--2", style: { alignContent: "start" } },
            stat({ label: "Total", icon: "groups", value: fmtNumber(total), accent: true }),
            stat({ label: "Active", icon: "how_to_reg", value: fmtNumber(rows.reduce((s, r) => s + Number(r.activeEmployees || 0), 0)) }),
            stat({ label: "On probation", icon: "hourglass_top", value: fmtNumber(rows.reduce((s, r) => s + Number(r.probationEmployees || 0), 0)) }),
            stat({ label: "Departments", icon: "apartment", value: fmtNumber(rows.length) })
          )
        ),
        dataTable({
          rows,
          searchable: false,
          headerActions: csvButton("headcount.csv", columns, rows),
          title: "Detail",
          columns: [
            { key: "departmentName", label: "Department" },
            { key: "totalEmployees", label: "Total", align: "right" },
            { key: "activeEmployees", label: "Active", align: "right" },
            { key: "probationEmployees", label: "Probation", align: "right" },
          ],
        })
      );
    }
  );

// ---------------------------------------------------------------- Attendance
function attendanceView() {
  const { start, end } = monthBounds();
  const from = compactInput({ label: "From", type: "date", value: start, onChange: () => body.reload() });
  const to = compactInput({ label: "To", type: "date", value: end, onChange: () => body.reload() });

  const body = asyncSection(
    async () => {
      const [start, end] = checkedRange(from, to);
      const [rows, departments] = await Promise.all([ReportsApi.attendance(start, end), lookups.departments().catch(() => [])]);
      return { rows, depts: new Map(departments.map((d) => [d.departmentId, d])) };
    },
    ({ rows, depts }) => {
      const columns = [
        { label: "Employee", value: (r) => r.employeeName },
        { label: "Department", value: (r) => departmentLabel(depts, r.departmentId) },
        { label: "Present", value: (r) => r.presentDays },
        { label: "Late", value: (r) => r.lateDays },
        { label: "Half day", value: (r) => r.halfDays },
        { label: "Absent", value: (r) => r.absentDays },
      ];
      return dataTable({
        rows,
        toolbarExtra: csvButton(`attendance_${from.control.value}_${to.control.value}.csv`, columns, rows),
        searchPlaceholder: "Search employees",
        searchText: (r) => `${r.employeeName} ${departmentLabel(depts, r.departmentId)}`,
        columns: [
          { key: "employeeName", label: "Employee" },
          { key: "departmentId", label: "Department", render: (r) => departmentLabel(depts, r.departmentId) },
          { key: "presentDays", label: "Present", align: "right" },
          { key: "lateDays", label: "Late", align: "right" },
          { key: "halfDays", label: "Half day", align: "right" },
          { key: "absentDays", label: "Absent", align: "right" },
        ],
        empty: { icon: "event_busy", title: "No attendance in this period" },
      });
    }
  );

  return h("div", null, h("div", { class: "filter-bar" }, from, to), body);
}

// ---------------------------------------------------------------- Leave
function leaveView() {
  const year = new Date().getFullYear();
  const yearControl = compactSelect({ label: "Year", value: year, options: [year + 1, year, year - 1, year - 2].map((y) => ({ value: y, label: String(y) })), onChange: () => body.reload() });

  const body = asyncSection(
    () => ReportsApi.leave(yearControl.control.value),
    (rows) => {
      const byType = new Map();
      rows.filter((r) => r.status === "APPROVED").forEach((r) => byType.set(r.leaveType, (byType.get(r.leaveType) || 0) + Number(r.totalDays || 0)));
      const columns = [
        { label: "Leave type", value: (r) => r.leaveType },
        { label: "Status", value: (r) => r.status },
        { label: "Requests", value: (r) => r.requests },
        { label: "Total days", value: (r) => r.totalDays },
      ];
      return h(
        "div",
        { class: "split" },
        card({ title: "Approved days by type" }, byType.size ? barList([...byType].map(([type, days]) => ({ label: humanize(type), value: days }))) : emptyState({ icon: "beach_access", title: "No approved leave" })),
        dataTable({
          rows,
          searchable: false,
          headerActions: csvButton(`leave_${yearControl.control.value}.csv`, columns, rows),
          title: "Requests by type and status",
          columns: [
            { key: "leaveType", label: "Type", render: (r) => humanize(r.leaveType) },
            { key: "status", label: "Status", render: (r) => statusBadge(r.status) },
            { key: "requests", label: "Requests", align: "right" },
            { key: "totalDays", label: "Days", align: "right" },
          ],
          empty: { icon: "beach_access", title: "No leave requests this year" },
        })
      );
    }
  );

  return h("div", null, h("div", { class: "filter-bar" }, yearControl), body);
}

// ---------------------------------------------------------------- Payroll
function payrollView() {
  const { start, end } = monthBounds();
  const from = compactInput({ label: "From", type: "date", value: start, onChange: () => body.reload() });
  const to = compactInput({ label: "To", type: "date", value: end, onChange: () => body.reload() });

  const body = asyncSection(
    () => ReportsApi.payroll(...checkedRange(from, to)),
    (rows) => {
      const active = rows.filter((r) => r.status !== "VOIDED");
      const sum = (key) => active.reduce((s, r) => s + Number(r[key] || 0), 0);
      const columns = [
        { label: "Status", value: (r) => r.status },
        { label: "Records", value: (r) => r.records },
        { label: "Base salary", value: (r) => r.totalBaseSalary },
        { label: "Overtime", value: (r) => r.totalOvertime },
        { label: "Deductions", value: (r) => r.totalDeductions },
        { label: "Net pay", value: (r) => r.totalNetPay },
      ];
      return h(
        "div",
        { class: "stack" },
        h(
          "div",
          { class: "grid grid--4" },
          stat({ label: "Net pay", icon: "account_balance", value: fmtMoney(sum("totalNetPay")), meta: "Excluding voided records", accent: true }),
          stat({ label: "Base salary", icon: "payments", value: fmtMoney(sum("totalBaseSalary")) }),
          stat({ label: "Overtime", icon: "more_time", value: fmtMoney(sum("totalOvertime")) }),
          stat({ label: "Deductions", icon: "remove_circle", value: fmtMoney(sum("totalDeductions")) })
        ),
        dataTable({
          rows,
          searchable: false,
          title: "By status",
          headerActions: csvButton(`payroll_${from.control.value}_${to.control.value}.csv`, columns, rows),
          columns: [
            { key: "status", label: "Status", render: (r) => statusBadge(r.status) },
            { key: "records", label: "Records", align: "right" },
            { key: "totalBaseSalary", label: "Base salary", align: "right", render: (r) => fmtMoney(r.totalBaseSalary) },
            { key: "totalOvertime", label: "Overtime", align: "right", render: (r) => fmtMoney(r.totalOvertime) },
            { key: "totalDeductions", label: "Deductions", align: "right", render: (r) => fmtMoney(r.totalDeductions) },
            { key: "totalNetPay", label: "Net pay", align: "right", render: (r) => fmtMoney(r.totalNetPay) },
          ],
          empty: { icon: "request_quote", title: "No payroll in this period" },
        })
      );
    }
  );

  return h("div", null, h("div", { class: "filter-bar" }, from, to), body);
}

// ---------------------------------------------------------------- Training
const trainingView = () =>
  asyncSection(
    () => ReportsApi.training(),
    (rows) => {
      const columns = [
        { label: "Program", value: (r) => r.title },
        { label: "Status", value: (r) => r.status },
        { label: "Capacity", value: (r) => r.capacity },
        { label: "Enrollments", value: (r) => r.totalEnrollments },
        { label: "Completed", value: (r) => r.completed },
        { label: "Dropped", value: (r) => r.dropped },
      ];
      return dataTable({
        rows,
        searchPlaceholder: "Search programs",
        searchText: (r) => r.title,
        toolbarExtra: csvButton("training.csv", columns, rows),
        columns: [
          { key: "title", label: "Program", render: (r) => h("span", { class: "title-small" }, r.title) },
          { key: "status", label: "Status", render: (r) => statusBadge(r.status) },
          {
            key: "totalEnrollments",
            label: "Enrollment",
            render: (r) =>
              h(
                "div",
                { class: "stack", style: { "--gap": "6px", minWidth: "140px" } },
                h("span", { class: "body-small muted num" }, r.capacity ? `${r.totalEnrollments} of ${r.capacity}` : `${r.totalEnrollments} enrolled`),
                r.capacity ? meter(Number(r.totalEnrollments), Number(r.capacity)) : null
              ),
            sortValue: (r) => Number(r.totalEnrollments),
          },
          { key: "completed", label: "Completed", align: "right" },
          { key: "dropped", label: "Dropped", align: "right" },
        ],
        empty: { icon: "school", title: "No training programs" },
      });
    }
  );

// ---------------------------------------------------------------- Performance
const performanceView = () =>
  asyncSection(
    async () => {
      const [rows, departments] = await Promise.all([ReportsApi.performance(), lookups.departments().catch(() => [])]);
      return { rows, depts: new Map(departments.map((d) => [d.departmentId, d])) };
    },
    ({ rows, depts }) => {
      const columns = [
        { label: "Employee", value: (r) => r.employeeName },
        { label: "Department", value: (r) => departmentLabel(depts, r.departmentId) },
        { label: "Reviews", value: (r) => r.reviews },
        { label: "Average rating", value: (r) => r.averageRating },
        { label: "Last review", value: (r) => r.lastReviewDate },
      ];
      return dataTable({
        rows,
        searchPlaceholder: "Search employees",
        searchText: (r) => `${r.employeeName} ${departmentLabel(depts, r.departmentId)}`,
        toolbarExtra: csvButton("performance.csv", columns, rows),
        columns: [
          { key: "employeeName", label: "Employee" },
          { key: "departmentId", label: "Department", render: (r) => departmentLabel(depts, r.departmentId) },
          { key: "reviews", label: "Reviews", align: "right" },
          { key: "averageRating", label: "Average", render: (r) => (r.averageRating != null ? h("span", { class: "row", style: { "--gap": "8px" } }, ratingStars(Math.round(Number(r.averageRating))), h("span", { class: "body-small muted num" }, Number(r.averageRating).toFixed(2))) : "—"), sortValue: (r) => Number(r.averageRating) },
          { key: "lastReviewDate", label: "Last review", render: (r) => fmtDate(r.lastReviewDate) },
        ],
        empty: { icon: "rate_review", title: "No reviews recorded" },
      });
    }
  );

// ---------------------------------------------------------------- Recruitment
const recruitmentView = () =>
  asyncSection(
    () => ReportsApi.recruitment(),
    (rows) => {
      const columns = [
        { label: "Vacancy", value: (r) => r.title },
        { label: "Status", value: (r) => r.status },
        { label: "Applications", value: (r) => r.applications },
        { label: "Shortlisted", value: (r) => r.shortlisted },
        { label: "Hired", value: (r) => r.hired },
      ];
      return dataTable({
        rows,
        searchPlaceholder: "Search vacancies",
        searchText: (r) => r.title,
        toolbarExtra: csvButton("recruitment.csv", columns, rows),
        columns: [
          { key: "title", label: "Vacancy", render: (r) => h("span", { class: "title-small" }, r.title) },
          { key: "status", label: "Status", render: (r) => statusBadge(r.status) },
          { key: "applications", label: "Applications", align: "right" },
          { key: "shortlisted", label: "Shortlisted", align: "right", render: (r) => r.shortlisted ?? 0 },
          { key: "hired", label: "Hired", align: "right", render: (r) => r.hired ?? 0 },
        ],
        empty: { icon: "work", title: "No vacancies" },
      });
    }
  );

export default function ReportsPage(ctx) {
  const { user } = ctx;
  const tabs = tabbedView({
    active: ctx.query.tab,
    items: [
      { id: "headcount", label: "Headcount", icon: "groups", visible: can(user, "reportHeadcount") },
      { id: "attendance", label: "Attendance", icon: "schedule", visible: can(user, "reportAttendance") && isOn("attendance") },
      { id: "leave", label: "Leave", icon: "beach_access", visible: can(user, "reportLeave") && isOn("leave") },
      { id: "payroll", label: "Payroll", icon: "payments", visible: can(user, "reportPayroll") && isOn("payroll") },
      { id: "training", label: "Training", icon: "school", visible: can(user, "reportTraining") },
      { id: "performance", label: "Performance", icon: "trending_up", visible: can(user, "reportPerformance") && isOn("performance") },
      { id: "recruitment", label: "Recruitment", icon: "work", visible: can(user, "reportRecruitment") },
    ],
    render: (id) =>
      ({
        headcount: headcountView,
        attendance: attendanceView,
        leave: leaveView,
        payroll: payrollView,
        training: trainingView,
        performance: performanceView,
        recruitment: recruitmentView,
      })[id](),
  });

  return page(pageHeader({ title: "Reports", subtitle: "Company-wide summaries. Date ranges default to the current month." }), tabs);
}
