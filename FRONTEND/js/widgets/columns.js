// Table column sets reused by feature pages and the employee detail tabs.

import { h } from "../core/dom.js";
import { fmtDate, fmtDateTime, fmtMoney, fmtNumber, fmtRange, fmtTime, humanize } from "../core/format.js";
import { departmentLabel, employeeLabel } from "../services/lookups.js";
import { personCell, ratingStars } from "../components/page.js";
import { statusBadge } from "../components/status.js";

export function hoursWorked(record) {
  if (!record.checkInTime || !record.checkOutTime) return null;
  const toMinutes = (t) => {
    const [hh, mm] = String(t).split(":").map(Number);
    return hh * 60 + mm;
  };
  const minutes = toMinutes(record.checkOutTime) - toMinutes(record.checkInTime);
  return minutes > 0 ? Math.round((minutes / 60) * 10) / 10 : null;
}

const note = (text, width = 240) =>
  text ? h("span", { class: "body-small muted truncate", title: text, style: { display: "inline-block", maxWidth: `${width}px`, verticalAlign: "middle" } }, text) : "—";

export const employeeColumn = (maps) => ({
  key: "employeeId",
  label: "Employee",
  render: (r) => personCell(employeeLabel(maps.employees, r.employeeId), departmentLabel(maps.departments, maps.employees.get(r.employeeId)?.departmentId)),
  sortValue: (r) => employeeLabel(maps.employees, r.employeeId),
});

const actionsColumn = (render) => render && { key: "actions", label: "", render };

export const attendanceColumns = ({ maps, withEmployee = false, actions } = {}) =>
  [
    withEmployee && employeeColumn(maps),
    { key: "eventDate", label: "Date", render: (r) => fmtDate(r.eventDate) },
    { key: "checkInTime", label: "Check in", render: (r) => fmtTime(r.checkInTime), cls: "num" },
    { key: "checkOutTime", label: "Check out", render: (r) => fmtTime(r.checkOutTime), cls: "num" },
    { key: "hours", label: "Hours", align: "right", render: (r) => hoursWorked(r) ?? "—", sortValue: hoursWorked },
    { key: "attendanceStatus", label: "Status", render: (r) => statusBadge(r.attendanceStatus) },
    { key: "overrideReason", label: "Note", render: (r) => note(r.overrideReason) },
    actionsColumn(actions),
  ].filter(Boolean);

export const leaveColumns = ({ maps, withEmployee = false, actions } = {}) =>
  [
    withEmployee && employeeColumn(maps),
    { key: "leaveType", label: "Type", render: (r) => humanize(r.leaveType) },
    { key: "startDate", label: "Dates", render: (r) => fmtRange(r.startDate, r.endDate) },
    // Working days: weekends and public holidays aren't counted
    { key: "leaveDays", label: "Work days", align: "right" },
    { key: "reason", label: "Reason", render: (r) => note(r.reason, 220) },
    { key: "leaveStatus", label: "Status", render: (r) => statusBadge(r.leaveStatus) },
    { key: "submittedDate", label: "Submitted", render: (r) => fmtDate(r.submittedDate) },
    actionsColumn(actions),
  ].filter(Boolean);

export const payrollColumns = ({ maps, withEmployee = false, detailed = true, actions } = {}) =>
  [
    withEmployee && employeeColumn(maps),
    { key: "payPeriodStart", label: "Pay period", render: (r) => fmtRange(r.payPeriodStart, r.payPeriodEnd) },
    detailed && { key: "baseSalary", label: "Base", align: "right", render: (r) => fmtMoney(r.baseSalary), sortValue: (r) => Number(r.baseSalary) },
    detailed && {
      key: "overtimeAmount",
      label: "Overtime",
      align: "right",
      render: (r) => h("span", { title: `${fmtNumber(r.overtimeHours)} hours` }, fmtMoney(r.overtimeAmount)),
      sortValue: (r) => Number(r.overtimeAmount),
    },
    detailed && { key: "deductions", label: "Deductions", align: "right", render: (r) => fmtMoney(r.deductions), sortValue: (r) => Number(r.deductions) },
    { key: "netPay", label: "Net pay", align: "right", render: (r) => h("strong", { style: { fontWeight: "500" } }, fmtMoney(r.netPay)), sortValue: (r) => Number(r.netPay) },
    { key: "status", label: "Status", render: (r) => statusBadge(r.status) },
    actionsColumn(actions),
  ].filter(Boolean);

export const reviewColumns = ({ maps, withEmployee = false, actions } = {}) =>
  [
    withEmployee && employeeColumn(maps),
    { key: "reviewDate", label: "Review date", render: (r) => fmtDate(r.reviewDate) },
    { key: "rating", label: "Rating", render: (r) => ratingStars(r.rating) },
    { key: "feedback", label: "Feedback", render: (r) => note(r.feedback, 360) },
    { key: "createdAt", label: "Recorded", render: (r) => fmtDateTime(r.createdAt) },
    actionsColumn(actions),
  ].filter(Boolean);

export const enrollmentColumns = ({ maps, programs, withEmployee = false, withProgram = true, actions } = {}) =>
  [
    withEmployee && employeeColumn(maps),
    withProgram && {
      key: "programId",
      label: "Program",
      render: (r) => {
        const p = programs?.get(r.programId);
        return h("div", null, h("div", null, p?.title ?? `Program #${r.programId}`), p && h("div", { class: "body-small muted" }, fmtRange(p.startDate, p.endDate)));
      },
      sortValue: (r) => programs?.get(r.programId)?.title,
    },
    { key: "enrolledDate", label: "Enrolled", render: (r) => fmtDate(r.enrolledDate) },
    { key: "completionStatus", label: "Status", render: (r) => statusBadge(r.completionStatus) },
    { key: "completionDate", label: "Completed", render: (r) => fmtDate(r.completionDate) },
    actionsColumn(actions),
  ].filter(Boolean);

export const byDateDesc = (key) => (a, b) => String(b[key] ?? "").localeCompare(String(a[key] ?? ""));
