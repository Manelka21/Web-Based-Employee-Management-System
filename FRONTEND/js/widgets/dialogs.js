// Create/edit dialogs used from more than one page.

import { h, icon } from "../core/dom.js";
import { fmtDateTime, fmtMoney, fmtNumber, fmtRange, fullName, humanize, monthBounds, toISODate, todayISO } from "../core/format.js";
import { all, daysBetweenInclusive, v, workingDays } from "../core/validators.js";
import { AttendanceApi, EmployeesApi, HolidaysApi, LeaveApi, PayrollApi, PerformanceApi, TrainingApi } from "../services/api.js";
import { lookups } from "../services/lookups.js";
import { confirmDialog, formDialog, openDialog } from "../components/dialog.js";
import { enumOptions } from "../components/fields.js";
import { button } from "../components/button.js";
import { statusBadge } from "../components/status.js";
import { toast, toastError } from "../components/snackbar.js";

export const EMPLOYEE_STATUSES = ["ACTIVE", "PROBATION", "ON_LEAVE", "INACTIVE"];
export const ATTENDANCE_STATUSES = ["PRESENT", "LATE", "HALF_DAY", "ABSENT"];
export const LEAVE_TYPES = ["ANNUAL", "CASUAL", "SICK", "MATERNITY", "OTHER"];

export const employeeOptions = (list) =>
  [...list].sort((a, b) => fullName(a).localeCompare(fullName(b))).map((e) => ({ value: e.employeeId, label: `${fullName(e)} (#${e.employeeId})` }));

// ---------------------------------------------------------------- Employees
export async function employeeDialog(existing) {
  const [departments, positions] = await Promise.all([lookups.departments(), lookups.positions()]);
  const deptOptions = departments
    .filter((d) => d.status === "ACTIVE" || d.departmentId === existing?.departmentId)
    .map((d) => ({ value: d.departmentId, label: d.name }));

  const latestHire = new Date();
  latestHire.setMonth(latestHire.getMonth() + 6);

  const fields = [
    { name: "firstName", label: "First name", required: true, value: existing?.firstName, maxLength: 80, validate: v.name("First name") },
    { name: "lastName", label: "Last name", required: true, value: existing?.lastName, maxLength: 80, validate: v.name("Last name") },
    { name: "email", label: "Work email", type: "email", required: true, value: existing?.email, maxLength: 120, normalize: "lower", validate: v.email },
    { name: "phone", label: "Phone", type: "tel", value: existing?.phone, maxLength: 20, normalize: "phone", help: "e.g. 0771234567", validate: v.phone },
    { name: "nic", label: "NIC", required: !existing, disabled: !!existing, value: existing?.nic, maxLength: 12, normalize: "upper", help: existing ? "NIC can't be changed" : "123456789V or 200012345678", validate: v.nic },
    existing
      ? { name: "status", label: "Status", type: "select", required: true, options: enumOptions(EMPLOYEE_STATUSES, humanize), value: existing.status }
      : { name: "hireDate", label: "Hire date", type: "date", required: true, value: todayISO(), min: "1970-01-01", max: toISODate(latestHire), help: "Up to 6 months ahead" },
    { name: "departmentId", label: "Department", type: "select", options: deptOptions, value: existing?.departmentId, parse: "int", emptyLabel: "Unassigned" },
    { name: "positionId", label: "Position", type: "select", options: positions.map((p) => ({ value: p.positionId, label: p.title })), value: existing?.positionId, parse: "int", emptyLabel: "Unassigned" },
    { name: "gender", label: "Gender", type: "select", options: enumOptions(["FEMALE", "MALE", "OTHER"], humanize), value: existing?.gender, emptyLabel: "Not recorded", help: "Needed for maternity leave" },
    !existing && { name: "status", label: "Starting status", type: "select", required: true, options: enumOptions(["ACTIVE", "PROBATION"], humanize), value: "ACTIVE" },
    { name: "address", label: "Address", type: "textarea", rows: 2, value: existing?.address, maxLength: 255, span: "all" },
  ].filter(Boolean);

  const result = await formDialog({
    title: existing ? `Edit ${fullName(existing)}` : "Add employee",
    fields,
    wide: true,
    submitLabel: existing ? "Save changes" : "Add employee",
    successMessage: existing ? "Employee updated" : (emp) => `${fullName(emp)} added`,
    // Positions belong to departments — only offer the ones that match
    onMount: (form) => {
      const dept = form.elements.namedItem("departmentId");
      const pos = form.elements.namedItem("positionId");
      const sync = () => {
        for (const opt of pos.options) {
          if (!opt.value) continue;
          const p = positions.find((x) => String(x.positionId) === opt.value);
          opt.hidden = !!dept.value && String(p?.departmentId) !== dept.value;
        }
        if (pos.selectedOptions[0]?.hidden) pos.value = "";
      };
      dept.addEventListener("change", sync);
      sync();
    },
    validate: (values) => ({ positionId: values.positionId && !values.departmentId ? "Choose a department first" : null }),
    onSubmit: (values) => {
      if (existing) {
        const { nic, ...changes } = values;
        // The API ignores null fields (partial update): a cleared phone/address is sent as "",
        // and a department/position switched to "Unassigned" is cleared with an explicit flag.
        return EmployeesApi.update(existing.employeeId, {
          ...changes,
          phone: changes.phone ?? "",
          address: changes.address ?? "",
          clearDepartment: existing.departmentId != null && changes.departmentId == null,
          clearPosition: existing.positionId != null && changes.positionId == null && changes.departmentId != null,
        });
      }
      return EmployeesApi.create(values);
    },
  });
  if (result) lookups.invalidate("employees", "team", "myEmployee");
  return result;
}

// Permanent delete. Because it can't be undone and removes history too, the
// user has to type the employee's full name to confirm.
export async function deleteEmployee(employee) {
  const name = fullName(employee);
  const deleted = await formDialog({
    title: `Delete ${name}?`,
    intro:
      "This permanently deletes the employee record together with their attendance, leave, training, reviews, payroll and salary. " +
      "Their login is disabled. This can't be undone. To keep their history, edit the record and set the status to Inactive instead.",
    fields: [
      {
        name: "confirmName",
        label: `Type "${name}" to confirm`,
        required: true,
        autocomplete: "off",
        span: "all",
        validate: (value) => (value.toLowerCase() !== name.toLowerCase() ? "The name doesn't match" : null),
      },
    ],
    submitLabel: "Delete permanently",
    successMessage: `${name} deleted`,
    onSubmit: () => EmployeesApi.remove(employee.employeeId),
  });
  if (!deleted) return false;
  lookups.invalidate("employees", "team", "myEmployee");
  return true;
}

// ---------------------------------------------------------------- Attendance
// Same rules as AttendanceServiceImpl.applyFields
function attendanceRules({ status, checkInTime, checkOutTime }) {
  if (status === "ABSENT") return {};
  return {
    checkInTime: !checkInTime ? "Required unless the employee was absent" : null,
    checkOutTime: checkInTime && checkOutTime && checkOutTime <= checkInTime ? "Must be after the check-in time" : null,
  };
}

function syncAbsent(form) {
  const status = form.elements.namedItem("status");
  const times = ["checkInTime", "checkOutTime"].map((n) => form.elements.namedItem(n));
  const sync = () => {
    const absent = status.value === "ABSENT";
    times.forEach((t) => {
      t.disabled = absent;
      if (absent) t.value = "";
    });
  };
  status.addEventListener("change", sync);
  sync();
}

export async function logAttendanceDialog({ employeeId } = {}) {
  const [employees, me] = await Promise.all([lookups.team(), lookups.myEmployee().catch(() => null)]);
  // Your own attendance goes through check-in/check-out, not this form
  const loggable = employees.filter((e) => e.status !== "INACTIVE" && e.employeeId !== me?.employeeId);
  return formDialog({
    title: "Log attendance",
    validate: attendanceRules,
    fields: [
      { name: "employeeId", label: "Employee", type: "select", required: true, options: employeeOptions(loggable), value: employeeId, parse: "int", span: "all", emptyLabel: "Choose an employee" },
      { name: "date", label: "Date", type: "date", required: true, value: todayISO(), max: todayISO() },
      { name: "status", label: "Status", type: "select", required: true, options: enumOptions(ATTENDANCE_STATUSES, humanize), value: "PRESENT" },
      { name: "checkInTime", label: "Check in", type: "time" },
      { name: "checkOutTime", label: "Check out", type: "time" },
    ],
    submitLabel: "Log",
    successMessage: "Attendance logged",
    onMount: syncAbsent,
    onSubmit: (values) => AttendanceApi.log(values),
  });
}

export function correctAttendanceDialog(record, employeeName) {
  return formDialog({
    title: "Correct attendance",
    intro: `${employeeName ? `${employeeName}, ` : ""}${fmtRange(record.eventDate)}. The employee is notified of the change.`,
    fields: [
      { name: "status", label: "Status", type: "select", required: true, options: enumOptions(ATTENDANCE_STATUSES, humanize), value: record.attendanceStatus, span: "all" },
      { name: "checkInTime", label: "Check in", type: "time", value: record.checkInTime },
      { name: "checkOutTime", label: "Check out", type: "time", value: record.checkOutTime },
      { name: "overrideReason", label: "Reason for correction", type: "textarea", rows: 2, required: true, maxLength: 255, span: "all", validate: v.minLength(5, "The reason") },
    ],
    validate: attendanceRules,
    submitLabel: "Save correction",
    successMessage: "Attendance corrected",
    onMount: syncAbsent,
    onSubmit: (values) => AttendanceApi.correct(record.eventId, values),
  });
}

export async function deleteAttendance(record) {
  const ok = await confirmDialog({ title: "Delete this attendance record?", message: `The entry for ${fmtRange(record.eventDate)} is removed permanently.`, confirmLabel: "Delete", danger: true });
  if (!ok) return false;
  try {
    await AttendanceApi.remove(record.eventId);
    toast("Attendance record deleted");
    return true;
  } catch (err) {
    toastError(err);
    return false;
  }
}

// ---------------------------------------------------------------- Leave
// Holidays for this year and next as a Map("yyyy-MM-dd" -> name)
export async function holidayMap() {
  const year = new Date().getFullYear();
  const lists = await Promise.all([year, year + 1].map((y) => HolidaysApi.list(y).catch(() => [])));
  return new Map(lists.flat().map((h) => [h.holidayDate, h.name]));
}

export async function applyLeaveDialog() {
  const [me, holidays, balance] = await Promise.all([
    lookups.myEmployee().catch(() => null),
    holidayMap(),
    LeaveApi.balance().catch(() => []),
  ]);
  // Maternity leave is only for female employees (the API enforces this too)
  const types = LEAVE_TYPES.filter((t) => t !== "MATERNITY" || me?.gender === "FEMALE");
  const thisYear = String(new Date().getFullYear());
  const remainingOf = (type) => balance.find((b) => b.leaveType === type)?.remainingDays ?? null;

  // Live summary under the dates: working days used, what was skipped, balance left
  const summary = h("div", { class: "banner banner--info span-all", role: "status", "aria-live": "polite" });
  const describe = ({ leaveType, startDate, endDate }) => {
    const days = workingDays(startDate, endDate, holidays);
    const skipped = [
      days.weekendDays && `${days.weekendDays} weekend day${days.weekendDays === 1 ? "" : "s"}`,
      days.holidays.length && `holiday${days.holidays.length === 1 ? "" : "s"}: ${days.holidays.join(", ")}`,
    ].filter(Boolean);
    const remaining = remainingOf(leaveType);
    const usedThisYear = days.byYear[thisYear] || 0;
    const over = remaining != null && usedThisYear > remaining;
    return { days, skipped, remaining, over };
  };

  const render = (form) => {
    const values = {
      leaveType: form.elements.namedItem("leaveType").value,
      startDate: form.elements.namedItem("startDate").value,
      endDate: form.elements.namedItem("endDate").value,
    };
    if (!values.startDate || !values.endDate || values.endDate < values.startDate) {
      summary.hidden = true;
      return;
    }
    const { days, skipped, remaining, over } = describe(values);
    summary.hidden = false;
    summary.className = `banner banner--${days.working === 0 || over ? "warning" : "info"} span-all`;
    summary.replaceChildren(
      icon(days.working === 0 || over ? "warning" : "event_available"),
      h(
        "div",
        null,
        h("strong", null, days.working === 0 ? "No working days in this range" : `Uses ${days.working} working day${days.working === 1 ? "" : "s"}`),
        skipped.length ? ` (not counted: ${skipped.join("; ")})` : "",
        remaining != null ? h("div", { class: "body-small" }, over ? `Only ${remaining} ${humanize(values.leaveType).toLowerCase()} day${remaining === 1 ? "" : "s"} left this year.` : `${remaining} ${humanize(values.leaveType).toLowerCase()} day${remaining === 1 ? "" : "s"} left this year before this request.`) : null
      )
    );
  };

  return formDialog({
    title: "Apply for leave",
    intro: "Requests go to your supervisor for approval. Weekends and public holidays don't use up your balance.",
    fields: [
      { name: "leaveType", label: "Leave type", type: "select", required: true, options: enumOptions(types, humanize), value: "ANNUAL", span: "all" },
      { name: "startDate", label: "From", type: "date", required: true, min: todayISO(), value: todayISO() },
      { name: "endDate", label: "To", type: "date", required: true, min: todayISO(), value: todayISO() },
      { name: "reason", label: "Reason", type: "textarea", rows: 3, maxLength: 500, span: "all" },
    ],
    extra: summary,
    submitLabel: "Submit request",
    successMessage: "Leave request submitted",
    onMount: (form) => {
      const start = form.elements.namedItem("startDate");
      const end = form.elements.namedItem("endDate");
      start.addEventListener("change", () => {
        end.min = start.value;
        if (end.value < start.value) end.value = start.value;
      });
      ["leaveType", "startDate", "endDate"].forEach((name) => form.elements.namedItem(name).addEventListener("change", () => render(form)));
      render(form);
    },
    // Same limits as LeaveServiceImpl.apply (the API re-checks every one)
    validate: ({ leaveType, startDate, endDate }) => {
      const oneYearAhead = new Date();
      oneYearAhead.setFullYear(oneYearAhead.getFullYear() + 1);
      const { days, remaining, over } = describe({ leaveType, startDate, endDate });
      return {
        startDate: startDate < todayISO() ? "Leave can't start in the past" : startDate > toISODate(oneYearAhead) ? "You can book at most one year ahead" : null,
        endDate:
          endDate < startDate
            ? "The end date can't be before the start date"
            : daysBetweenInclusive(startDate, endDate) > 90
              ? "One request can't be longer than 90 days"
              : days.working === 0
                ? "These dates are all weekends or public holidays"
                : over
                  ? `Not enough balance: ${remaining} day${remaining === 1 ? "" : "s"} left this year`
                  : null,
      };
    },
    onSubmit: (values) => LeaveApi.apply(values),
  });
}

// ---------------------------------------------------------------- Payroll
// Deductions can't exceed gross pay (base + overtime at 1.5× the hourly rate of base/160),
// the same rule as PayrollServiceImpl.checkDeductions. When overtime is left blank the
// server works it out from attendance, so only base salary is checked here.
function deductionRule({ baseSalary, overtimeHours, deductions }) {
  if (baseSalary == null || deductions == null) return {};
  const overtime = overtimeHours != null ? (baseSalary / 160) * 1.5 * overtimeHours : 0;
  const gross = Math.round((baseSalary + overtime) * 100) / 100;
  return { deductions: deductions > gross ? `Can't be more than gross pay (${fmtMoney(gross)})` : null };
}

export async function generatePayrollDialog({ employeeId } = {}) {
  const [employees, me] = await Promise.all([lookups.employees(), lookups.myEmployee().catch(() => null)]);
  const { start, end } = monthBounds();
  // Separation of duties: nobody generates their own payroll
  const payable = employees.filter((e) => e.status !== "INACTIVE" && e.employeeId !== me?.employeeId);
  return formDialog({
    title: "Generate payroll",
    wide: true,
    fields: [
      { name: "employeeId", label: "Employee", type: "select", required: true, options: employeeOptions(payable), value: employeeId, parse: "int", span: "all", emptyLabel: "Choose an employee" },
      { name: "payPeriodStart", label: "Period start", type: "date", required: true, value: start },
      { name: "payPeriodEnd", label: "Period end", type: "date", required: true, value: end },
      { name: "baseSalary", label: "Base salary (LKR)", type: "number", min: 0.01, max: 9999999999.99, step: "0.01", inputmode: "decimal", help: "Filled in from the salary on file", validate: all(v.range(0.01, 9999999999.99, "Base salary"), v.maxDecimals(2, "Base salary")) },
      { name: "deductions", label: "Deductions (LKR)", type: "number", min: 0, step: "0.01", inputmode: "decimal", validate: all(v.range(0, null, "Deductions"), v.maxDecimals(2, "Deductions")) },
      { name: "overtimeHours", label: "Overtime hours", type: "number", min: 0, max: 200, step: "0.25", inputmode: "decimal", span: "all", help: "Leave blank to calculate from attendance (time beyond 8 hours a day)", validate: all(v.range(0, 200, "Overtime hours"), v.maxDecimals(2, "Overtime hours")) },
    ],
    submitLabel: "Generate",
    successMessage: (r) => `Draft created · net pay ${fmtMoney(r.netPay)}`,
    // Pre-fill the base salary from the salary on file whenever the employee changes
    onMount: (form) => {
      const employee = form.elements.namedItem("employeeId");
      const salary = form.elements.namedItem("baseSalary");
      const support = salary.closest(".field").querySelector(".field__support");
      const load = async () => {
        if (!employee.value) return;
        try {
          const onFile = await PayrollApi.salary(employee.value);
          salary.value = onFile.baseSalary;
          support.textContent = `Salary on file: ${fmtMoney(onFile.baseSalary)}`;
        } catch {
          salary.value = "";
          support.textContent = "No salary on file. Enter one, or set it under Payroll → Salaries.";
        }
      };
      employee.addEventListener("change", load);
      load();
    },
    validate: (values) => ({
      baseSalary: values.baseSalary == null ? "Enter a base salary (no salary is on file for this employee)" : null,
      payPeriodEnd:
        values.payPeriodEnd < values.payPeriodStart
          ? "The period end can't be before the start"
          : daysBetweenInclusive(values.payPeriodStart, values.payPeriodEnd) > 31
            ? "A pay period can't be longer than 31 days"
            : null,
      ...deductionRule(values),
    }),
    onSubmit: (values) => PayrollApi.generate(values),
  });
}

export function editPayrollDialog(record) {
  return formDialog({
    title: "Correct draft payroll",
    intro: `Pay period ${fmtRange(record.payPeriodStart, record.payPeriodEnd)}. Net pay is recalculated when you save.`,
    fields: [
      { name: "baseSalary", label: "Base salary (LKR)", type: "number", required: true, min: 0.01, step: "0.01", value: record.baseSalary, span: "all", validate: all(v.range(0.01, 9999999999.99, "Base salary"), v.maxDecimals(2, "Base salary")) },
      { name: "overtimeHours", label: "Overtime hours", type: "number", min: 0, max: 200, step: "0.25", value: record.overtimeHours, validate: all(v.range(0, 200, "Overtime hours"), v.maxDecimals(2, "Overtime hours")) },
      { name: "deductions", label: "Deductions (LKR)", type: "number", min: 0, step: "0.01", value: record.deductions, validate: all(v.range(0, null, "Deductions"), v.maxDecimals(2, "Deductions")) },
    ],
    validate: (values) => deductionRule({ ...values, overtimeHours: values.overtimeHours ?? record.overtimeHours }),
    successMessage: (r) => `Saved · net pay ${fmtMoney(r.netPay)}`,
    onSubmit: (values) => PayrollApi.update(record.payrollId, values),
  });
}

export function payslipDialog(record, employeeName) {
  const line = (label, value, strong = false) =>
    h("div", { class: "row row--between" }, h("span", { class: strong ? "title-medium" : "body-medium muted" }, label), h("span", { class: strong ? "headline-small num" : "body-medium num" }, value));

  openDialog({
    title: "Payslip",
    content: h(
      "div",
      { class: "stack", style: { "--gap": "20px", color: "var(--md-on-surface)" } },
      h(
        "div",
        { class: "row row--between row--wrap" },
        h("div", null, employeeName && h("div", { class: "title-medium" }, employeeName), h("div", { class: "body-medium muted" }, fmtRange(record.payPeriodStart, record.payPeriodEnd))),
        statusBadge(record.status)
      ),
      h(
        "div",
        { class: "card card--filled stack", style: { "--gap": "12px" } },
        line("Base salary", fmtMoney(record.baseSalary)),
        line(`Overtime · ${fmtNumber(record.overtimeHours)} h`, fmtMoney(record.overtimeAmount)),
        line("Deductions", `− ${fmtMoney(record.deductions)}`),
        h("hr", { class: "divider" }),
        line("Net pay", fmtMoney(record.netPay), true)
      ),
      h("p", { class: "body-small muted" }, `Payroll #${record.payrollId} · generated ${fmtDateTime(record.generatedAt)}`)
    ),
    actions: (close) => [button({ label: "Close", variant: "text", onClick: () => close() })],
  });
}

export function salaryDialog(employee, current) {
  return formDialog({
    title: current ? "Change salary" : "Set salary",
    intro: `${fullName(employee)} · monthly base salary. New payroll drafts use this amount.`,
    fields: [
      { name: "baseSalary", label: "Base salary (LKR)", type: "number", required: true, min: 0.01, step: "0.01", inputmode: "decimal", value: current?.baseSalary, span: "all", validate: all(v.range(0.01, 9999999999.99, "Base salary"), v.maxDecimals(2, "Base salary")) },
    ],
    successMessage: "Salary saved",
    onSubmit: ({ baseSalary }) => PayrollApi.setSalary(employee.employeeId, baseSalary),
  });
}

// ---------------------------------------------------------------- Performance
const RATING_OPTIONS = [
  { value: 5, label: "5 · Excellent" },
  { value: 4, label: "4 · Very good" },
  { value: 3, label: "3 · Good" },
  { value: 2, label: "2 · Needs improvement" },
  { value: 1, label: "1 · Unsatisfactory" },
];

export async function reviewDialog({ existing, employeeId, selfEmployeeId } = {}) {
  const team = (await lookups.team()).filter((e) => e.employeeId !== selfEmployeeId && e.status !== "INACTIVE");
  return formDialog({
    title: existing ? "Edit review" : "Record a review",
    wide: true,
    fields: [
      { name: "employeeId", label: "Employee", type: "select", required: true, options: employeeOptions(team), value: existing?.employeeId ?? employeeId, parse: "int", disabled: !!existing, emptyLabel: "Choose a team member" },
      { name: "reviewDate", label: "Review date", type: "date", required: true, value: existing?.reviewDate ?? todayISO(), max: todayISO() },
      { name: "rating", label: "Rating", type: "select", options: RATING_OPTIONS, value: existing?.rating, parse: "int", emptyLabel: "No rating", span: "all" },
      { name: "feedback", label: "Feedback", type: "textarea", rows: 5, required: true, maxLength: 1000, value: existing?.feedback, span: "all", help: "At least 10 characters. The employee can read this.", validate: v.minLength(10, "Feedback") },
    ],
    submitLabel: existing ? "Save" : "Record review",
    successMessage: existing ? "Review updated" : "Review recorded",
    onSubmit: (values) => {
      const body = { ...values, employeeId: existing?.employeeId ?? values.employeeId };
      return existing ? PerformanceApi.update(existing.performanceId, body) : PerformanceApi.create(body);
    },
  });
}

export async function deleteReview(review) {
  const ok = await confirmDialog({ title: "Delete this review?", message: "The employee will no longer see this feedback.", confirmLabel: "Delete", danger: true });
  if (!ok) return false;
  try {
    await PerformanceApi.remove(review.performanceId);
    toast("Review deleted");
    return true;
  } catch (err) {
    toastError(err);
    return false;
  }
}

// ---------------------------------------------------------------- Training
export async function enrolDialog({ employeeId, programId } = {}) {
  const [programs, team] = await Promise.all([TrainingApi.programs(), lookups.team()]);
  const open = programs.filter((p) => ["SCHEDULED", "IN_PROGRESS"].includes(p.status));
  return formDialog({
    title: "Enrol in training",
    fields: [
      { name: "employeeId", label: "Employee", type: "select", required: true, options: employeeOptions(team.filter((e) => e.status !== "INACTIVE")), value: employeeId, parse: "int", span: "all", emptyLabel: "Choose an employee" },
      { name: "programId", label: "Program", type: "select", required: true, options: open.map((p) => ({ value: p.programId, label: `${p.title} · ${fmtRange(p.startDate, p.endDate)}` })), value: programId, parse: "int", span: "all", emptyLabel: "Choose a program" },
    ],
    submitLabel: "Enrol",
    successMessage: "Enrolled",
    onSubmit: ({ employeeId: emp, programId: prog }) => TrainingApi.enrol(emp, prog),
  });
}

export async function enrollmentAction(enrollment, kind) {
  const complete = kind === "complete";
  const ok = await confirmDialog({
    title: complete ? "Mark as completed?" : "Drop this enrollment?",
    message: complete ? "The completion date is set to today." : "The record is kept with the status Dropped.",
    confirmLabel: complete ? "Mark completed" : "Drop",
    danger: !complete,
  });
  if (!ok) return false;
  try {
    await (complete ? TrainingApi.complete(enrollment.enrollmentId) : TrainingApi.drop(enrollment.enrollmentId));
    toast(complete ? "Marked as completed" : "Enrollment dropped");
    return true;
  } catch (err) {
    toastError(err);
    return false;
  }
}
