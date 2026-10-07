// Recruitment & onboarding (HR): vacancies and candidate applications.
// Moving an application to HIRED creates a probationary employee record.

import { h } from "../core/dom.js";
import { fmtDate, fullName, todayISO } from "../core/format.js";
import { takeAction } from "../core/router.js";
import { isOn } from "../core/features.js";
import { all, v } from "../core/validators.js";
import { ApplicationsApi, VacanciesApi } from "../services/api.js";
import { departmentLabel, lookups } from "../services/lookups.js";
import { asyncSection } from "../components/feedback.js";
import { button, iconButton } from "../components/button.js";
import { confirmDialog, formDialog, openDialog } from "../components/dialog.js";
import { compactSelect } from "../components/fields.js";
import { openMenu } from "../components/menu.js";
import { dataTable } from "../components/table.js";
import { tabbedView } from "../components/tabs.js";
import { kv, page, pageHeader, personCell } from "../components/page.js";
import { statusBadge } from "../components/status.js";
import { toast, toastError } from "../components/snackbar.js";

const PIPELINE = ["APPLIED", "SHORTLISTED", "INTERVIEWED", "HIRED"];
const TERMINAL = new Set(["HIRED", "REJECTED", "WITHDRAWN"]);

async function run(task, message) {
  try {
    const result = await task();
    if (message) toast(message);
    return result ?? true;
  } catch (err) {
    toastError(err);
    return false;
  }
}

// ---------------------------------------------------------------- Dialogs
async function vacancyDialog(existing) {
  const departments = await lookups.departments();
  return formDialog({
    title: existing ? "Edit vacancy" : "Post a vacancy",
    wide: true,
    fields: [
      { name: "title", label: "Job title", required: true, maxLength: 120, value: existing?.title, span: "all", validate: v.minLength(3, "The job title") },
      { name: "departmentId", label: "Department", type: "select", required: true, options: departments.filter((d) => d.status === "ACTIVE").map((d) => ({ value: d.departmentId, label: d.name })), value: existing?.departmentId, parse: "int", emptyLabel: "Choose a department" },
      { name: "deadline", label: "Application deadline", type: "date", min: todayISO(), value: existing?.deadline },
      { name: "requirements", label: "Requirements", type: "textarea", rows: 4, maxLength: 1000, value: existing?.requirements, span: "all" },
    ],
    submitLabel: existing ? "Save" : "Post vacancy",
    successMessage: existing ? "Vacancy updated" : "Vacancy posted",
    onSubmit: (values) => (existing ? VacanciesApi.update(existing.vacancyId, values) : VacanciesApi.create(values)),
  });
}

async function applicationDialog(vacancyId) {
  const vacancies = (await VacanciesApi.list("OPEN")) || [];
  return formDialog({
    title: "Add candidate",
    wide: true,
    fields: [
      { name: "vacancyId", label: "Vacancy", type: "select", required: true, options: vacancies.map((v) => ({ value: v.vacancyId, label: v.title })), value: vacancyId, parse: "int", emptyLabel: vacancies.length ? "Choose a vacancy" : "No open vacancies", span: "all" },
      { name: "candidateName", label: "Full name", required: true, maxLength: 120, validate: all(v.minLength(2, "The name"), v.name("Full name")) },
      { name: "candidateEmail", label: "Email", type: "email", required: true, maxLength: 120, normalize: "lower", validate: v.email },
      { name: "candidatePhone", label: "Phone", type: "tel", maxLength: 20, normalize: "phone", help: "e.g. 0771234567", validate: v.phone },
      { name: "candidateNic", label: "NIC", maxLength: 12, normalize: "upper", help: "Required before hiring", validate: v.nic },
      { name: "appliedDate", label: "Applied on", type: "date", value: todayISO(), max: todayISO() },
      { name: "resumeNotes", label: "Résumé notes", type: "textarea", rows: 3, maxLength: 1000, span: "all" },
    ],
    submitLabel: "Add candidate",
    successMessage: "Candidate added",
    onSubmit: (values) => ApplicationsApi.create(values),
  });
}

function editCandidateDialog(app) {
  return formDialog({
    title: `Edit ${app.candidateName}`,
    intro: "Correct the candidate's details, for example add the NIC needed for hiring.",
    wide: true,
    fields: [
      { name: "candidateName", label: "Full name", required: true, maxLength: 120, value: app.candidateName, validate: all(v.minLength(2, "The name"), v.name("Full name")) },
      { name: "candidateEmail", label: "Email", type: "email", required: true, maxLength: 120, normalize: "lower", value: app.candidateEmail, validate: v.email },
      { name: "candidatePhone", label: "Phone", type: "tel", maxLength: 20, normalize: "phone", value: app.candidatePhone, validate: v.phone },
      { name: "candidateNic", label: "NIC", maxLength: 12, normalize: "upper", value: app.candidateNic, help: "Required before hiring", validate: v.nic },
      { name: "resumeNotes", label: "Résumé notes", type: "textarea", rows: 3, maxLength: 1000, value: app.resumeNotes, span: "all" },
    ],
    successMessage: "Candidate updated",
    onSubmit: (values) => ApplicationsApi.update(app.applicationId, values),
  });
}

function applicationDetails(app, vacancyTitle) {
  openDialog({
    title: app.candidateName,
    content: h(
      "div",
      { class: "stack", style: { color: "var(--md-on-surface)" } },
      h("div", { class: "row row--wrap" }, statusBadge(app.status), h("span", { class: "body-medium muted" }, vacancyTitle)),
      kv([
        ["Email", app.candidateEmail],
        ["Phone", app.candidatePhone || "—"],
        ["NIC", app.candidateNic || "—"],
        ["Applied", fmtDate(app.appliedDate)],
      ]),
      h("div", null, h("div", { class: "kv__key" }, "Résumé notes"), h("p", { class: "body-medium pretty", style: { whiteSpace: "pre-line", marginTop: "4px" } }, app.resumeNotes || "—"))
    ),
    actions: (close) => [button({ label: "Close", variant: "text", onClick: () => close() })],
  });
}

async function moveApplication(app, status, ctx) {
  // The API needs a NIC to create the employee record; say so before asking to confirm
  if (status === "HIRED" && !app.candidateNic) {
    toastError(new Error(`Add ${app.candidateName}'s NIC before hiring (More actions → Edit details).`));
    return false;
  }
  if (status === "HIRED") {
    const ok = await confirmDialog({
      title: `Hire ${app.candidateName}?`,
      message: "This creates an employee record on probation in the vacancy's department. It can't be undone from here.",
      confirmLabel: "Hire",
      icon: "how_to_reg",
    });
    if (!ok) return false;
  }
  if (status === "REJECTED" && !(await confirmDialog({ title: `Reject ${app.candidateName}?`, message: "The application is closed as rejected.", confirmLabel: "Reject", danger: true }))) return false;

  const response = await run(() => ApplicationsApi.setStatus(app.applicationId, status));
  if (!response) return false;

  const hired = response.createdEmployee;
  if (hired) {
    lookups.invalidate("employees");
    // The employee record is always created; "View" needs the directory switched on
    toast(`${fullName(hired)} hired and added to the directory`, {
      action: isOn("employees") ? { label: "View", onClick: () => ctx.navigate(`/employees/${hired.employeeId}`) } : undefined,
      duration: 8000,
    });
  } else {
    toast(`Moved to ${status.charAt(0) + status.slice(1).toLowerCase()}`);
  }
  return true;
}

// ---------------------------------------------------------------- Page
export default function RecruitmentPage(ctx) {
  const action = takeAction(ctx);
  let vacancyFilter = ctx.query.vacancy || "";
  let tabs;

  const vacanciesView = () =>
    asyncSection(
      async () => {
        const [vacancies, applications, departments] = await Promise.all([VacanciesApi.list(), ApplicationsApi.list(), lookups.departments()]);
        const counts = new Map();
        applications.forEach((a) => counts.set(a.vacancyId, (counts.get(a.vacancyId) || 0) + 1));
        return { vacancies, counts, depts: new Map(departments.map((d) => [d.departmentId, d])) };
      },
      ({ vacancies, counts, depts }, reload) =>
        dataTable({
          title: "Vacancies",
          headerActions: button({ label: "Post vacancy", icon: "add", variant: "tonal", size: "sm", onClick: async () => (await vacancyDialog()) && reload() }),
          rows: vacancies,
          searchPlaceholder: "Search vacancies",
          searchText: (v) => `${v.title} ${departmentLabel(depts, v.departmentId)} ${v.requirements ?? ""}`,
          filters: [
            { id: "all", label: "All" },
            { id: "OPEN", label: "Open", predicate: (v) => v.status === "OPEN" },
            { id: "CLOSED", label: "Closed", predicate: (v) => v.status === "CLOSED" },
            { id: "FILLED", label: "Filled", predicate: (v) => v.status === "FILLED" },
          ],
          onRowClick: (v) => {
            vacancyFilter = String(v.vacancyId);
            tabs.select("applications");
          },
          columns: [
            { key: "title", label: "Vacancy", render: (v) => h("div", null, h("div", { class: "title-small" }, v.title), h("div", { class: "body-small muted" }, departmentLabel(depts, v.departmentId))) },
            { key: "deadline", label: "Deadline", render: (v) => fmtDate(v.deadline) },
            { key: "applications", label: "Applications", align: "right", render: (v) => counts.get(v.vacancyId) || 0, sortValue: (v) => counts.get(v.vacancyId) || 0 },
            { key: "status", label: "Status", render: (v) => statusBadge(v.status) },
            {
              key: "actions",
              render: (v) => [
                v.status === "OPEN" && iconButton({ icon: "edit", label: "Edit", size: "sm", onClick: async () => (await vacancyDialog(v)) && reload() }),
                v.status !== "FILLED" &&
                  iconButton({
                    icon: "more_vert",
                    label: "More actions",
                    size: "sm",
                    onClick: (event) =>
                      openMenu(event.currentTarget, {
                        items: [
                          v.status === "OPEN" && { label: "Close vacancy", icon: "lock", onClick: async () => (await run(() => VacanciesApi.close(v.vacancyId), "Vacancy closed")) && reload() },
                          v.status === "CLOSED" && !(v.deadline && v.deadline < todayISO()) && { label: "Reopen", icon: "lock_open", onClick: async () => (await run(() => VacanciesApi.setStatus(v.vacancyId, "OPEN"), "Vacancy reopened")) && reload() },
                          { label: "Mark as filled", icon: "task_alt", onClick: async () => (await run(() => VacanciesApi.setStatus(v.vacancyId, "FILLED"), "Vacancy marked as filled")) && reload() },
                          { divider: true },
                          {
                            label: "Delete",
                            icon: "delete",
                            onClick: async () => {
                              if (!(await confirmDialog({ title: `Delete “${v.title}”?`, message: "Vacancies that already have applications can't be deleted — close them instead.", confirmLabel: "Delete", danger: true }))) return;
                              (await run(() => VacanciesApi.remove(v.vacancyId), "Vacancy deleted")) && reload();
                            },
                          },
                        ].filter(Boolean),
                      }),
                  }),
              ],
            },
          ],
          empty: { icon: "work", title: "No vacancies yet", text: "Post a vacancy to start collecting applications." },
        })
    );

  const applicationsView = () =>
    asyncSection(
      async () => {
        const [applications, vacancies] = await Promise.all([ApplicationsApi.list(), VacanciesApi.list()]);
        return { applications, vacancies: new Map(vacancies.map((v) => [v.vacancyId, v])) };
      },
      ({ applications, vacancies }, reload) => {
        const vacancyTitle = (id) => vacancies.get(id)?.title ?? `Vacancy #${id}`;
        const filterControl = compactSelect({
          label: "Vacancy",
          emptyLabel: "All vacancies",
          value: vacancyFilter,
          options: [...vacancies.values()].map((v) => ({ value: v.vacancyId, label: v.title })),
          onChange: (value) => {
            vacancyFilter = value;
            table.setRows(visible());
          },
        });
        const visible = () => (vacancyFilter ? applications.filter((a) => String(a.vacancyId) === String(vacancyFilter)) : applications);

        const table = dataTable({
          title: "Applications",
          headerActions: button({ label: "Add candidate", icon: "person_add", variant: "tonal", size: "sm", onClick: async () => (await applicationDialog(vacancyFilter ? Number(vacancyFilter) : undefined)) && reload() }),
          rows: visible(),
          toolbarExtra: filterControl,
          searchPlaceholder: "Search candidates",
          searchText: (a) => `${a.candidateName} ${a.candidateEmail} ${a.candidateNic ?? ""} ${vacancyTitle(a.vacancyId)}`,
          filters: [
            { id: "all", label: "All" },
            { id: "active", label: "In progress", predicate: (a) => !TERMINAL.has(a.status) },
            { id: "HIRED", label: "Hired", predicate: (a) => a.status === "HIRED" },
            { id: "closed", label: "Rejected or withdrawn", predicate: (a) => a.status === "REJECTED" || a.status === "WITHDRAWN" },
          ],
          onRowClick: (a) => applicationDetails(a, vacancyTitle(a.vacancyId)),
          columns: [
            { key: "candidateName", label: "Candidate", render: (a) => personCell(a.candidateName, a.candidateEmail) },
            { key: "vacancyId", label: "Vacancy", render: (a) => vacancyTitle(a.vacancyId), sortValue: (a) => vacancyTitle(a.vacancyId) },
            { key: "appliedDate", label: "Applied", render: (a) => fmtDate(a.appliedDate) },
            { key: "status", label: "Stage", render: (a) => statusBadge(a.status) },
            {
              key: "actions",
              render: (a) => {
                if (TERMINAL.has(a.status)) return null;
                const next = PIPELINE.slice(PIPELINE.indexOf(a.status) + 1);
                return [
                  button({
                    label: next[0] === "HIRED" ? "Hire" : next[0] === "INTERVIEWED" ? "Interviewed" : "Shortlist",
                    variant: "tonal",
                    size: "sm",
                    onClick: async () => (await moveApplication(a, next[0], ctx)) && reload(),
                  }),
                  iconButton({
                    icon: "more_vert",
                    label: "More actions",
                    size: "sm",
                    onClick: (event) =>
                      openMenu(event.currentTarget, {
                        items: [
                          ...next.slice(1).map((s) => ({ label: s === "HIRED" ? "Hire" : `Move to ${s.charAt(0) + s.slice(1).toLowerCase()}`, icon: s === "HIRED" ? "how_to_reg" : "arrow_forward", onClick: async () => (await moveApplication(a, s, ctx)) && reload() })),
                          { label: "Edit details", icon: "edit", onClick: async () => (await editCandidateDialog(a)) && reload() },
                          { label: "Reject", icon: "cancel", onClick: async () => (await moveApplication(a, "REJECTED", ctx)) && reload() },
                          { divider: true },
                          {
                            label: "Withdraw",
                            icon: "undo",
                            onClick: async () => {
                              if (!(await confirmDialog({ title: `Withdraw ${a.candidateName}'s application?`, message: "The record is kept with the status Withdrawn.", confirmLabel: "Withdraw" }))) return;
                              (await run(() => ApplicationsApi.withdraw(a.applicationId), "Application withdrawn")) && reload();
                            },
                          },
                        ],
                      }),
                  }),
                ];
              },
            },
          ],
          empty: { icon: "person_search", title: "No applications yet", text: "Candidates you add appear here with their hiring stage." },
        });
        return table;
      }
    );

  tabs = tabbedView({
    active: ctx.query.tab,
    items: [
      { id: "vacancies", label: "Vacancies", icon: "work" },
      { id: "applications", label: "Applications", icon: "person_search" },
    ],
    render: (id) => (id === "applications" ? applicationsView() : vacanciesView()),
  });

  if (action === "new") {
    setTimeout(async () => {
      const created = ctx.query.tab === "applications" ? await applicationDialog() : await vacancyDialog();
      if (created) ctx.reload();
    });
  }

  return page(pageHeader({ title: "Recruitment", subtitle: "Vacancies, candidates and hiring" }), tabs);
}
