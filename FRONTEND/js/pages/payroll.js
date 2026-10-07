import { fmtDateTime, fmtMoney, fmtRange, fullName } from "../core/format.js";
import { personCell } from "../components/page.js";
import { can, isLinked } from "../core/permissions.js";
import { takeAction } from "../core/router.js";
import { PayrollApi } from "../services/api.js";
import { employeeLabel, lookups } from "../services/lookups.js";
import { asyncSection } from "../components/feedback.js";
import { button, iconButton } from "../components/button.js";
import { confirmDialog } from "../components/dialog.js";
import { dataTable } from "../components/table.js";
import { tabbedView } from "../components/tabs.js";
import { page, pageHeader } from "../components/page.js";
import { toast, toastError } from "../components/snackbar.js";
import { byDateDesc, payrollColumns } from "../widgets/columns.js";
import { editPayrollDialog, generatePayrollDialog, payslipDialog, salaryDialog } from "../widgets/dialogs.js";

const TRANSITIONS = {
  finalize: { title: "Finalize payroll?", message: "The employee is notified and can see this payslip. It can no longer be edited.", confirm: "Finalize", run: PayrollApi.finalize, done: "Payroll finalized" },
  pay: { title: "Mark as paid?", message: "Confirms the salary has been transferred. The employee is notified.", confirm: "Mark paid", run: PayrollApi.pay, done: "Marked as paid" },
  void: { title: "Void this payroll record?", message: "The record is kept for audit with the status Voided. Paid records can't be voided.", confirm: "Void", run: PayrollApi.void, done: "Payroll voided", danger: true },
};

async function transition(record, kind, name) {
  const t = TRANSITIONS[kind];
  const ok = await confirmDialog({ title: t.title, message: `${name} · ${fmtRange(record.payPeriodStart, record.payPeriodEnd)} · ${fmtMoney(record.netPay)}. ${t.message}`, confirmLabel: t.confirm, danger: t.danger });
  if (!ok) return false;
  try {
    await t.run(record.payrollId);
    toast(t.done);
    return true;
  } catch (err) {
    toastError(err);
    return false;
  }
}

export default function PayrollPage(ctx) {
  const { user } = ctx;
  const action = takeAction(ctx);
  const linked = isLinked(user);
  const canRun = can(user, "runPayroll");

  const generate = async () => {
    if (await generatePayrollDialog()) ctx.navigate("/payroll", { tab: "all" });
  };

  const myView = () =>
    asyncSection(
      async () => (await PayrollApi.mine()).sort(byDateDesc("payPeriodEnd")),
      (rows) =>
        dataTable({
          title: "My payslips",
          subtitle: "Finalized and paid payslips",
          rows,
          searchable: false,
          columns: payrollColumns({}),
          onRowClick: (r) => payslipDialog(r, user.fullName),
          empty: { icon: "receipt_long", title: "No payslips yet", text: "Payslips appear here once payroll is finalized." },
        })
    );

  const allView = () =>
    asyncSection(
      async () => {
        const [records, maps] = await Promise.all([PayrollApi.list(), lookups.maps()]);
        return { records: records.sort(byDateDesc("generatedAt")), maps };
      },
      ({ records, maps }, reload) => {
        const name = (r) => employeeLabel(maps.employees, r.employeeId);
        return dataTable({
          rows: records,
          searchPlaceholder: "Search employees",
          searchText: name,
          filters: [
            { id: "all", label: "All" },
            ...["DRAFT", "FINALIZED", "PAID", "VOIDED"].map((s) => ({ id: s, label: s.charAt(0) + s.slice(1).toLowerCase(), predicate: (r) => r.status === s })),
          ],
          onRowClick: (r) => payslipDialog(r, name(r)),
          columns: payrollColumns({
            maps,
            withEmployee: true,
            actions: canRun
              ? (r) => [
                  r.status === "DRAFT" && iconButton({ icon: "edit", label: "Correct draft", size: "sm", onClick: async () => (await editPayrollDialog(r)) && reload() }),
                  r.status === "DRAFT" && button({ label: "Finalize", variant: "tonal", size: "sm", onClick: async () => (await transition(r, "finalize", name(r))) && reload() }),
                  r.status === "FINALIZED" && button({ label: "Mark paid", variant: "tonal", size: "sm", onClick: async () => (await transition(r, "pay", name(r))) && reload() }),
                  ["DRAFT", "FINALIZED"].includes(r.status) && iconButton({ icon: "block", label: "Void", size: "sm", onClick: async () => (await transition(r, "void", name(r))) && reload() }),
                ]
              : null,
          }),
          empty: {
            icon: "request_quote",
            title: "No payroll records yet",
            text: canRun ? "Generate a payroll record for an employee and pay period." : "Payroll records will appear here once generated.",
            action: canRun ? { label: "Generate payroll", icon: "add", onClick: generate } : undefined,
          },
        });
      }
    );

  // Salaries on file (HR and Payroll only; supervisors never see pay)
  const salariesView = () =>
    asyncSection(
      async () => {
        const [salaries, employees, me] = await Promise.all([PayrollApi.salaries(), lookups.employees(), lookups.myEmployee().catch(() => null)]);
        const byEmployee = new Map(salaries.map((s) => [s.employeeId, s]));
        const rows = employees
          .filter((e) => e.status !== "INACTIVE")
          .map((e) => ({ employee: e, salary: byEmployee.get(e.employeeId) }));
        return { rows, selfId: me?.employeeId };
      },
      ({ rows, selfId }, reload) =>
        dataTable({
          rows,
          searchPlaceholder: "Search employees",
          searchText: (r) => `${fullName(r.employee)} ${r.employee.email}`,
          filters: [
            { id: "all", label: "All" },
            { id: "missing", label: "No salary on file", predicate: (r) => !r.salary },
          ],
          columns: [
            { key: "name", label: "Employee", render: (r) => personCell(fullName(r.employee), r.employee.email), sortValue: (r) => fullName(r.employee) },
            { key: "salary", label: "Base salary", align: "right", render: (r) => (r.salary ? fmtMoney(r.salary.baseSalary) : "—"), sortValue: (r) => Number(r.salary?.baseSalary ?? -1) },
            { key: "updated", label: "Last changed", render: (r) => fmtDateTime(r.salary?.updatedAt) },
            {
              key: "actions",
              // Nobody sets their own pay (the API refuses too)
              render: (r) =>
                r.employee.employeeId !== selfId &&
                iconButton({ icon: "edit", label: r.salary ? "Change salary" : "Set salary", size: "sm", onClick: async () => (await salaryDialog(r.employee, r.salary)) && reload() }),
            },
          ],
          empty: { icon: "payments", title: "No employees" },
        })
    );

  const tabs = tabbedView({
    active: ctx.query.tab,
    items: [
      { id: "mine", label: "My payslips", icon: "receipt_long", visible: linked },
      { id: "all", label: "All records", icon: "table_view", visible: can(user, "viewAllPayroll") },
      { id: "salaries", label: "Salaries", icon: "savings", visible: can(user, "manageSalaries") },
    ],
    render: (id) => (id === "all" ? allView() : id === "salaries" ? salariesView() : myView()),
  });

  if (action === "generate" && canRun) setTimeout(generate);

  return page(
    pageHeader({
      title: "Payroll",
      subtitle: canRun ? "Draft → Finalized → Paid. Generation is refused while leave in the period is still pending." : "Salary records and payslips",
      actions: canRun ? button({ label: "Generate payroll", icon: "add", onClick: generate }) : null,
    }),
    tabs
  );
}
