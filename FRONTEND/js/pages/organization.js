// Departments and job positions. Everyone can read; HR can change.

import { h } from "../core/dom.js";
import { fmtDate, fullName } from "../core/format.js";
import { can } from "../core/permissions.js";
import { takeAction } from "../core/router.js";
import { v } from "../core/validators.js";
import { isOn } from "../core/features.js";
import { DepartmentsApi, HolidaysApi, PositionsApi } from "../services/api.js";
import { compactSelect } from "../components/fields.js";
import { departmentLabel, employeeLabel, lookups } from "../services/lookups.js";
import { asyncSection } from "../components/feedback.js";
import { button, iconButton } from "../components/button.js";
import { confirmDialog, formDialog } from "../components/dialog.js";
import { dataTable } from "../components/table.js";
import { tabbedView } from "../components/tabs.js";
import { page, pageHeader } from "../components/page.js";
import { statusBadge } from "../components/status.js";
import { toast, toastError } from "../components/snackbar.js";
import { employeeOptions } from "../widgets/dialogs.js";

async function departmentDialog(existing) {
  const employees = await lookups.employees().catch(() => []);
  // Business rule (DepartmentServiceImpl.validateHead): the head must be an active
  // employee and, for an existing department, a member of it.
  const headCandidates = employees.filter(
    (e) => e.status !== "INACTIVE" && (!existing || e.departmentId === existing.departmentId)
  );
  const result = await formDialog({
    title: existing ? `Edit ${existing.name}` : "New department",
    fields: [
      { name: "name", label: "Name", required: true, maxLength: 120, value: existing?.name, span: "all", validate: v.minLength(2, "The name") },
      { name: "description", label: "Description", type: "textarea", rows: 3, maxLength: 500, value: existing?.description, span: "all" },
      { name: "headOfDeptId", label: "Head of department", type: "select", options: employeeOptions(headCandidates), value: existing?.headOfDeptId, parse: "int", emptyLabel: "Not assigned", span: existing ? null : "all", help: existing ? "Active members of this department" : null },
      existing && { name: "status", label: "Status", type: "select", required: true, options: [{ value: "ACTIVE", label: "Active" }, { value: "INACTIVE", label: "Inactive" }], value: existing.status },
    ].filter(Boolean),
    submitLabel: existing ? "Save" : "Create",
    successMessage: existing ? "Department updated" : (d) => `${d.name} created`,
    onSubmit: (values) => (existing ? DepartmentsApi.update(existing.departmentId, values) : DepartmentsApi.create(values)),
  });
  if (result) lookups.invalidate("departments");
  return result;
}

async function positionDialog(existing) {
  const departments = await lookups.departments();
  const result = await formDialog({
    title: existing ? `Edit ${existing.title}` : "New position",
    fields: [
      { name: "title", label: "Title", required: true, maxLength: 120, value: existing?.title, span: "all", validate: v.minLength(2, "The title") },
      { name: "departmentId", label: "Department", type: "select", required: true, options: departments.filter((d) => d.status === "ACTIVE" || d.departmentId === existing?.departmentId).map((d) => ({ value: d.departmentId, label: d.name })), value: existing?.departmentId, parse: "int", emptyLabel: "Choose a department", span: "all" },
      { name: "description", label: "Description", type: "textarea", rows: 3, maxLength: 500, value: existing?.description, span: "all" },
    ],
    submitLabel: existing ? "Save" : "Create",
    successMessage: existing ? "Position updated" : (p) => `${p.title} created`,
    onSubmit: (values) => (existing ? PositionsApi.update(existing.positionId, values) : PositionsApi.create(values)),
  });
  if (result) lookups.invalidate("positions");
  return result;
}

async function confirmAndRun({ title, message, confirmLabel, run, success }) {
  if (!(await confirmDialog({ title, message, confirmLabel, danger: true }))) return false;
  try {
    await run();
    toast(success);
    return true;
  } catch (err) {
    toastError(err);
    return false;
  }
}

export default function OrganizationPage(ctx) {
  const { user } = ctx;
  const isHr = can(user, "manageOrganization");
  const action = takeAction(ctx);
  lookups.invalidate("departments", "positions");

  let tabs;

  const departments = () =>
    asyncSection(
      async () => {
        const [list, maps] = await Promise.all([DepartmentsApi.list(), lookups.maps()]);
        return { list, maps };
      },
      ({ list, maps }, reload) =>
        dataTable({
          rows: list,
          searchPlaceholder: "Search departments",
          headerActions: isHr ? button({ label: "New department", icon: "add", variant: "tonal", size: "sm", onClick: async () => (await departmentDialog()) && reload() }) : null,
          title: "Departments",
          filters: [
            { id: "all", label: "All" },
            { id: "ACTIVE", label: "Active", predicate: (d) => d.status === "ACTIVE" },
            { id: "INACTIVE", label: "Inactive", predicate: (d) => d.status === "INACTIVE" },
          ],
          columns: [
            { key: "name", label: "Name", render: (d) => h("span", { class: "title-small" }, d.name) },
            { key: "description", label: "Description", render: (d) => h("span", { class: "body-medium muted" }, d.description || "—") },
            { key: "headOfDeptId", label: "Head", render: (d) => (d.headOfDeptId ? employeeLabel(maps.employees, d.headOfDeptId) : "—") },
            { key: "status", label: "Status", render: (d) => statusBadge(d.status) },
            isHr && {
              key: "actions",
              render: (d) => [
                iconButton({ icon: "edit", label: `Edit ${d.name}`, size: "sm", onClick: async () => (await departmentDialog(d)) && reload() }),
                iconButton({
                  icon: "delete",
                  label: `Delete ${d.name}`,
                  size: "sm",
                  onClick: async () =>
                    (await confirmAndRun({
                      title: `Delete ${d.name}?`,
                      message:
                        "The department and its job positions are deleted permanently. Only empty departments can be deleted: " +
                        "move its employees and remove its vacancies and training programs first. To keep it for history, edit it and set it to Inactive instead.",
                      confirmLabel: "Delete",
                      run: () => DepartmentsApi.remove(d.departmentId),
                      success: `${d.name} deleted`,
                    })) && (lookups.invalidate("departments", "positions"), reload()),
                }),
              ],
            },
          ].filter(Boolean),
          empty: { icon: "apartment", title: "No departments yet" },
        })
    );

  const positions = () =>
    asyncSection(
      async () => {
        const [list, depts] = await Promise.all([PositionsApi.list(), lookups.departments()]);
        return { list, depts: new Map(depts.map((d) => [d.departmentId, d])) };
      },
      ({ list, depts }, reload) =>
        dataTable({
          rows: list,
          title: "Job positions",
          searchPlaceholder: "Search positions",
          searchText: (p) => `${p.title} ${departmentLabel(depts, p.departmentId)} ${p.description ?? ""}`,
          headerActions: isHr ? button({ label: "New position", icon: "add", variant: "tonal", size: "sm", onClick: async () => (await positionDialog()) && reload() }) : null,
          columns: [
            { key: "title", label: "Title", render: (p) => h("span", { class: "title-small" }, p.title) },
            { key: "departmentId", label: "Department", render: (p) => departmentLabel(depts, p.departmentId), sortValue: (p) => departmentLabel(depts, p.departmentId) },
            { key: "description", label: "Description", render: (p) => h("span", { class: "body-medium muted" }, p.description || "—") },
            isHr && {
              key: "actions",
              render: (p) => [
                iconButton({ icon: "edit", label: `Edit ${p.title}`, size: "sm", onClick: async () => (await positionDialog(p)) && reload() }),
                iconButton({
                  icon: "delete",
                  label: `Delete ${p.title}`,
                  size: "sm",
                  onClick: async () =>
                    (await confirmAndRun({
                      title: `Delete ${p.title}?`,
                      message: "Positions still held by employees can't be deleted.",
                      confirmLabel: "Delete",
                      run: () => PositionsApi.remove(p.positionId),
                      success: "Position deleted",
                    })) && (lookups.invalidate("positions"), reload()),
                }),
              ],
            },
          ].filter(Boolean),
          empty: { icon: "work_outline", title: "No positions yet" },
        })
    );

  // Public holidays: leave requests don't count these days
  const holidays = () => {
    let year = new Date().getFullYear();
    const yearControl = compactSelect({
      label: "Year",
      value: year,
      options: [year - 1, year, year + 1, year + 2].map((y) => ({ value: y, label: String(y) })),
      onChange: (value) => {
        year = Number(value);
        list.reload();
      },
    });

    const addHoliday = () =>
      formDialog({
        title: "Add a holiday",
        intro: "Weekdays marked as holidays don't use up anyone's leave.",
        fields: [
          { name: "holidayDate", label: "Date", type: "date", required: true, min: `${new Date().getFullYear()}-01-01`, span: "all" },
          { name: "name", label: "Name", required: true, maxLength: 120, span: "all", help: "e.g. Vesak Full Moon Poya Day", validate: v.minLength(3, "The name") },
        ],
        validate: ({ holidayDate }) => {
          const day = new Date(`${holidayDate}T00:00`).getDay();
          return { holidayDate: day === 0 || day === 6 ? "Weekends never count as leave days, so there's no need to add them" : null };
        },
        submitLabel: "Add holiday",
        successMessage: (h) => `${h.name} added`,
        onSubmit: (values) => HolidaysApi.add(values),
      });

    const list = asyncSection(
      () => HolidaysApi.list(year),
      (rows, reload) =>
        dataTable({
          rows,
          title: "Public holidays",
          subtitle: "Poya and other lunar holidays change every year. Add them here.",
          searchable: false,
          toolbarExtra: yearControl,
          headerActions: isHr ? button({ label: "Add holiday", icon: "add", variant: "tonal", size: "sm", onClick: async () => (await addHoliday()) && reload() }) : null,
          columns: [
            { key: "holidayDate", label: "Date", render: (r) => `${fmtDate(r.holidayDate)} · ${new Date(`${r.holidayDate}T00:00`).toLocaleDateString("en-GB", { weekday: "long" })}` },
            { key: "name", label: "Holiday" },
            isHr && {
              key: "actions",
              render: (r) =>
                iconButton({
                  icon: "delete",
                  label: `Remove ${r.name}`,
                  size: "sm",
                  onClick: async () =>
                    (await confirmAndRun({
                      title: `Remove ${r.name}?`,
                      message: "Leave already booked over this date will start counting it as a working day.",
                      confirmLabel: "Remove",
                      run: () => HolidaysApi.remove(r.holidayDate),
                      success: "Holiday removed",
                    })) && reload(),
                }),
            },
          ].filter(Boolean),
          empty: { icon: "event", title: `No holidays recorded for ${year}` },
        })
    );
    return list;
  };

  tabs = tabbedView({
    active: action === "new-department" ? "departments" : ctx.query.tab,
    items: [
      { id: "departments", label: "Departments", icon: "apartment" },
      { id: "positions", label: "Positions", icon: "work_outline" },
      // Public holidays belong to Leave, so they follow its switch. Departments and positions are always on.
      { id: "holidays", label: "Holidays", icon: "event", visible: isOn("leave") },
    ],
    render: (id) => (id === "positions" ? positions() : id === "holidays" ? holidays() : departments()),
  });

  if (action === "new-department" && isHr) setTimeout(async () => (await departmentDialog()) && ctx.reload());

  return page(pageHeader({ title: "Organization", subtitle: "Departments and the positions within them" }), tabs);
}
