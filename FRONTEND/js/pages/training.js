import { h } from "../core/dom.js";
import { fmtRange, humanize, todayISO } from "../core/format.js";
import { v } from "../core/validators.js";
import { can, isLinked } from "../core/permissions.js";
import { takeAction } from "../core/router.js";
import { TrainingApi } from "../services/api.js";
import { departmentLabel, lookups } from "../services/lookups.js";
import { asyncSection, emptyState } from "../components/feedback.js";
import { button, iconButton, withBusy } from "../components/button.js";
import { confirmDialog, formDialog, openDialog } from "../components/dialog.js";
import { compactSelect } from "../components/fields.js";
import { openMenu } from "../components/menu.js";
import { dataTable } from "../components/table.js";
import { tabbedView } from "../components/tabs.js";
import { kv, meter, page, pageHeader } from "../components/page.js";
import { statusBadge } from "../components/status.js";
import { toast, toastError } from "../components/snackbar.js";
import { byDateDesc, enrollmentColumns } from "../widgets/columns.js";
import { enrolDialog, enrollmentAction } from "../widgets/dialogs.js";

const OPEN = new Set(["SCHEDULED", "IN_PROGRESS"]);

async function programDialog(existing) {
  const departments = await lookups.departments();
  return formDialog({
    title: existing ? "Edit program" : "New training program",
    wide: true,
    fields: [
      { name: "title", label: "Title", required: true, maxLength: 120, value: existing?.title, span: "all", validate: v.minLength(3, "The title") },
      { name: "trainer", label: "Trainer", maxLength: 120, value: existing?.trainer },
      { name: "departmentId", label: "Audience", type: "select", options: departments.filter((d) => d.status === "ACTIVE" || d.departmentId === existing?.departmentId).map((d) => ({ value: d.departmentId, label: d.name })), value: existing?.departmentId, parse: "int", emptyLabel: "Company-wide" },
      { name: "startDate", label: "Starts", type: "date", value: existing?.startDate, min: existing ? null : todayISO(), validate: existing ? null : v.notPast("The start date") },
      { name: "endDate", label: "Ends", type: "date", value: existing?.endDate },
      { name: "capacity", label: "Capacity", type: "number", min: 1, max: 500, step: 1, value: existing?.capacity, parse: "int", help: "Leave blank for no limit", validate: v.range(1, 500, "Capacity") },
      existing && { name: "status", label: "Status", type: "select", required: true, options: ["SCHEDULED", "IN_PROGRESS", "COMPLETED"].map((s) => ({ value: s, label: humanize(s) })), value: existing.status },
      { name: "description", label: "Description", type: "textarea", rows: 3, maxLength: 500, value: existing?.description, span: "all" },
    ].filter(Boolean),
    submitLabel: existing ? "Save" : "Create program",
    successMessage: existing ? "Program updated" : "Program created",
    validate: ({ startDate, endDate }) => ({ endDate: startDate && endDate && endDate < startDate ? "The end date can't be before the start date" : null }),
    onSubmit: (values) => (existing ? TrainingApi.updateProgram(existing.programId, values) : TrainingApi.createProgram(values)),
  });
}

async function runAction(task, message) {
  try {
    await task();
    toast(message);
    return true;
  } catch (err) {
    toastError(err);
    return false;
  }
}

export default function TrainingPage(ctx) {
  const { user } = ctx;
  const action = takeAction(ctx);
  const isHr = can(user, "manageTraining");
  const linked = isLinked(user);
  const manageEnrollments = can(user, "manageEnrollments");

  const programsView = () =>
    asyncSection(
      async () => {
        const [programs, departments, mine] = await Promise.all([TrainingApi.programs(), lookups.departments(), linked ? TrainingApi.myEnrollments().catch(() => []) : []]);
        return {
          programs: programs.sort(byDateDesc("startDate")),
          depts: new Map(departments.map((d) => [d.departmentId, d])),
          enrolled: new Map(mine.map((e) => [e.programId, e])),
        };
      },
      ({ programs, depts, enrolled }, reload) =>
        dataTable({
          title: "Programs",
          headerActions: isHr ? button({ label: "New program", icon: "add", variant: "tonal", size: "sm", onClick: async () => (await programDialog()) && reload() }) : null,
          rows: programs,
          searchPlaceholder: "Search programs",
          searchText: (p) => `${p.title} ${p.trainer ?? ""} ${p.description ?? ""}`,
          filters: [
            { id: "all", label: "All" },
            { id: "open", label: "Open for enrollment", predicate: (p) => OPEN.has(p.status) },
            { id: "COMPLETED", label: "Completed", predicate: (p) => p.status === "COMPLETED" },
            { id: "CANCELLED", label: "Cancelled", predicate: (p) => p.status === "CANCELLED" },
          ],
          onRowClick: (p) => programDetails(p, depts),
          columns: [
            { key: "title", label: "Program", render: (p) => h("div", null, h("div", { class: "title-small" }, p.title), h("div", { class: "body-small muted" }, p.trainer ? `with ${p.trainer}` : "Trainer to be confirmed")) },
            { key: "startDate", label: "Dates", render: (p) => fmtRange(p.startDate, p.endDate) },
            { key: "departmentId", label: "Audience", render: (p) => (p.departmentId ? departmentLabel(depts, p.departmentId) : "Company-wide") },
            { key: "capacity", label: "Capacity", align: "right", render: (p) => p.capacity ?? "—" },
            { key: "status", label: "Status", render: (p) => statusBadge(p.status) },
            {
              key: "actions",
              render: (p) => {
                const mine = enrolled.get(p.programId);
                const items = [
                  linked && mine && statusBadge(mine.completionStatus, mine.completionStatus === "ENROLLED" ? "You're enrolled" : undefined),
                  linked &&
                    !mine &&
                    OPEN.has(p.status) &&
                    button({
                      label: "Enrol",
                      variant: "tonal",
                      size: "sm",
                      onClick: async (event) => {
                        try {
                          await withBusy(event.currentTarget, () => TrainingApi.selfEnrol(p.programId));
                          toast(`You're enrolled in ${p.title}`);
                          reload();
                        } catch (err) {
                          toastError(err);
                        }
                      },
                    }),
                  can(user, "enrolOthers") && OPEN.has(p.status) && iconButton({ icon: "person_add", label: "Enrol an employee", size: "sm", onClick: async () => (await enrolDialog({ programId: p.programId })) && reload() }),
                  isHr &&
                    iconButton({
                      icon: "more_vert",
                      label: "More actions",
                      size: "sm",
                      onClick: (event) =>
                        openMenu(event.currentTarget, {
                          items: [
                            OPEN.has(p.status) && { label: "Edit", icon: "edit", onClick: async () => (await programDialog(p)) && reload() },
                            OPEN.has(p.status) && {
                              label: "Cancel program",
                              icon: "event_busy",
                              onClick: async () =>
                                (await confirmDialog({ title: `Cancel ${p.title}?`, message: "Enrolled employees keep their records, but the program can no longer run.", confirmLabel: "Cancel program", cancelLabel: "Keep", danger: true })) &&
                                (await runAction(() => TrainingApi.cancelProgram(p.programId), "Program cancelled")) &&
                                reload(),
                            },
                            { divider: true },
                            {
                              label: "Delete",
                              icon: "delete",
                              onClick: async () =>
                                (await confirmDialog({ title: `Delete ${p.title}?`, message: "Programs with enrollments can't be deleted — cancel them instead.", confirmLabel: "Delete", danger: true })) &&
                                (await runAction(() => TrainingApi.deleteProgram(p.programId), "Program deleted")) &&
                                reload(),
                            },
                          ].filter(Boolean),
                        }),
                    }),
                ].filter(Boolean);
                return items;
              },
            },
          ],
          empty: { icon: "school", title: "No training programs yet", text: isHr ? "Create a program so employees can enrol." : "Programs will appear here when HR schedules them." },
        })
    );

  function programDetails(p, depts) {
    openDialog({
      title: p.title,
      wide: true,
      content: h(
        "div",
        { class: "stack", style: { "--gap": "20px", color: "var(--md-on-surface)" } },
        h("div", { class: "row row--wrap" }, statusBadge(p.status)),
        p.description && h("p", { class: "body-medium pretty" }, p.description),
        kv([
          ["Trainer", p.trainer || "—"],
          ["Dates", fmtRange(p.startDate, p.endDate)],
          ["Audience", p.departmentId ? departmentLabel(depts, p.departmentId) : "Company-wide"],
          ["Capacity", p.capacity ?? "No limit"],
        ]),
        manageEnrollments &&
          asyncSection(
            async () => {
              const [rows, maps] = await Promise.all([TrainingApi.byProgram(p.programId), lookups.maps()]);
              return { rows, maps };
            },
            ({ rows, maps }) => {
              const active = rows.filter((r) => r.completionStatus !== "DROPPED").length;
              return h(
                "div",
                { class: "stack", style: { "--gap": "8px" } },
                h("div", { class: "row row--between" }, h("span", { class: "title-small" }, "Enrollments"), h("span", { class: "body-small muted" }, p.capacity ? `${active} of ${p.capacity} seats` : `${active} enrolled`)),
                p.capacity && meter(active, p.capacity, { warn: active >= p.capacity }),
                rows.length
                  ? h("div", { class: "list" }, rows.map((r) => h("div", { class: "list-item", style: { padding: "8px 0" } }, h("div", { class: "list-item__body" }, maps.employees.get(r.employeeId) ? `${maps.employees.get(r.employeeId).firstName} ${maps.employees.get(r.employeeId).lastName}` : `Employee #${r.employeeId}`), statusBadge(r.completionStatus))))
                  : h("p", { class: "body-medium muted" }, "No one has enrolled yet.")
              );
            }
          )
      ),
      actions: (close) => [button({ label: "Close", variant: "text", onClick: () => close() })],
    });
  }

  const myView = () =>
    asyncSection(
      async () => {
        const [mine, programs] = await Promise.all([TrainingApi.myEnrollments(), TrainingApi.programs()]);
        return { mine: mine.sort(byDateDesc("enrolledDate")), programs: new Map(programs.map((p) => [p.programId, p])) };
      },
      ({ mine, programs }) =>
        dataTable({
          rows: mine,
          searchable: false,
          title: "My enrollments",
          filters: [
            { id: "all", label: "All" },
            { id: "active", label: "Active", predicate: (e) => ["ENROLLED", "IN_PROGRESS"].includes(e.completionStatus) },
            { id: "COMPLETED", label: "Completed", predicate: (e) => e.completionStatus === "COMPLETED" },
          ],
          columns: enrollmentColumns({ programs }),
          empty: { icon: "school", title: "You're not enrolled in any training", text: "Browse the Programs tab and enrol in one that's open." },
        })
    );

  const enrollmentsView = () =>
    asyncSection(
      async () => (await TrainingApi.programs()).sort(byDateDesc("startDate")),
      (programs) => {
        if (!programs.length) return emptyState({ icon: "school", title: "No programs yet" });
        let programId = programs.find((p) => OPEN.has(p.status))?.programId ?? programs[0].programId;
        const host = h("div");
        const load = () =>
          host.replaceChildren(
            asyncSection(
              async () => {
                const [rows, maps] = await Promise.all([TrainingApi.byProgram(programId), lookups.maps()]);
                return { rows, maps };
              },
              ({ rows, maps }, reload) =>
                dataTable({
                  rows,
                  toolbarExtra: select,
                  searchPlaceholder: "Search people",
                  searchText: (r) => {
                    const e = maps.employees.get(r.employeeId);
                    return e ? `${e.firstName} ${e.lastName} ${e.email}` : "";
                  },
                  filters: [
                    { id: "all", label: "All" },
                    { id: "active", label: "Active", predicate: (e) => ["ENROLLED", "IN_PROGRESS"].includes(e.completionStatus) },
                    { id: "COMPLETED", label: "Completed", predicate: (e) => e.completionStatus === "COMPLETED" },
                    { id: "DROPPED", label: "Dropped", predicate: (e) => e.completionStatus === "DROPPED" },
                  ],
                  columns: enrollmentColumns({
                    maps,
                    withEmployee: true,
                    withProgram: false,
                    actions: (r) =>
                      ["ENROLLED", "IN_PROGRESS"].includes(r.completionStatus) && [
                        iconButton({ icon: "task_alt", label: "Mark completed", size: "sm", onClick: async () => (await enrollmentAction(r, "complete")) && reload() }),
                        iconButton({ icon: "remove_circle", label: "Drop", size: "sm", onClick: async () => (await enrollmentAction(r, "drop")) && reload() }),
                      ],
                  }),
                  empty: {
                    icon: "group_add",
                    title: "No enrollments for this program",
                    action: can(user, "enrolOthers") ? { label: "Enrol an employee", icon: "person_add", onClick: async () => (await enrolDialog({ programId })) && reload() } : undefined,
                  },
                })
            )
          );
        const select = compactSelect({
          label: "Program",
          value: programId,
          options: programs.map((p) => ({ value: p.programId, label: p.title })),
          onChange: (value) => {
            programId = Number(value);
            load();
          },
        });
        load();
        return host;
      }
    );

  const tabs = tabbedView({
    active: ctx.query.tab,
    items: [
      { id: "programs", label: "Programs", icon: "school" },
      { id: "mine", label: "My training", icon: "person", visible: linked },
      { id: "enrollments", label: "Enrollments", icon: "groups", visible: manageEnrollments },
    ],
    render: (id) => (id === "mine" ? myView() : id === "enrollments" ? enrollmentsView() : programsView()),
  });

  if (action === "new" && isHr) setTimeout(async () => (await programDialog()) && ctx.reload());

  return page(pageHeader({ title: "Training", subtitle: "Learning programs and who's taking part" }), tabs);
}
