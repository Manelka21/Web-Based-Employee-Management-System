// IT administration: user accounts, roles, employee links, password resets and
// access requests made from the sign-in page.

import { h } from "../../core/dom.js";
import { fmtDate, fmtDateTime } from "../../core/format.js";
import { ALL_ROLES, ROLE_LABELS } from "../../core/permissions.js";
import { takeAction } from "../../core/router.js";
import { v } from "../../core/validators.js";
import { UsersApi } from "../../services/api.js";
import { openAccountRequests } from "../../widgets/accounts.js";
import { asyncSection } from "../../components/feedback.js";
import { button, iconButton } from "../../components/button.js";
import { confirmDialog, formDialog } from "../../components/dialog.js";
import { openMenu } from "../../components/menu.js";
import { dataTable } from "../../components/table.js";
import { page, pageHeader, personCell } from "../../components/page.js";
import { toast, toastError } from "../../components/snackbar.js";

const roleOptions = ALL_ROLES.map((role) => ({ value: role, label: ROLE_LABELS[role] }));

const createUserDialog = () =>
  formDialog({
    title: "New user account",
    wide: true,
    fields: [
      { name: "firstName", label: "First name", required: true, maxLength: 80, validate: v.name("First name") },
      { name: "lastName", label: "Last name", required: true, maxLength: 80, validate: v.name("Last name") },
      { name: "email", label: "Email", type: "email", required: true, maxLength: 120, normalize: "lower", span: "all", validate: v.email },
      { name: "phoneNumber", label: "Phone", type: "tel", maxLength: 20, normalize: "phone", validate: v.phone },
      { name: "role", label: "Role", type: "select", required: true, options: roleOptions, value: "EMPLOYEE" },
      { name: "password", label: "Temporary password", type: "password", required: true, minLength: 8, maxLength: 72, autocomplete: "new-password", help: "8+ characters with a letter and a number. Share it securely.", validate: v.password },
      { name: "confirm", label: "Confirm password", type: "password", required: true, maxLength: 72, autocomplete: "new-password" },
    ],
    submitLabel: "Create account",
    successMessage: (u) => `Account created for ${u.fullName}`,
    validate: ({ password, confirm }) => ({ confirm: password !== confirm ? "Those passwords didn't match" : null }),
    onSubmit: ({ confirm, ...values }) => UsersApi.create(values),
  });

const roleDialog = (user) =>
  formDialog({
    title: "Change role",
    intro: `${user.fullName} · ${user.email}. The new role applies to their very next request.`,
    fields: [{ name: "role", label: "Role", type: "select", required: true, options: roleOptions, value: user.role, span: "all" }],
    successMessage: "Role updated",
    onSubmit: ({ role }) => UsersApi.changeRole(user.userId, role),
  });

const linkDialog = (user) =>
  formDialog({
    title: user.employeeId != null ? "Change employee link" : "Link employee record",
    intro: `Linking gives ${user.fullName} self-service access to leave, attendance and payslips. For a supervisor, the linked record's department becomes their team.`,
    fields: [
      { name: "employeeId", label: "Employee ID", type: "number", required: true, min: 1, step: 1, parse: "int", value: user.employeeId, span: "all", help: "The number shown on the employee's record in the Directory (HR can tell you)" },
    ],
    submitLabel: "Link",
    successMessage: "Employee record linked",
    onSubmit: ({ employeeId }) => UsersApi.linkEmployee(user.userId, employeeId),
  });

const passwordDialog = (user) =>
  formDialog({
    title: "Reset password",
    intro: `Set a temporary password for ${user.fullName}. They'll have to choose their own the next time they sign in, and any lock on the account is lifted.`,
    fields: [
      { name: "newPassword", label: "New password", type: "password", required: true, minLength: 8, maxLength: 72, autocomplete: "new-password", span: "all", help: "8+ characters with a letter and a number", validate: v.password },
      { name: "confirm", label: "Confirm password", type: "password", required: true, maxLength: 72, autocomplete: "new-password", span: "all" },
    ],
    submitLabel: "Reset password",
    successMessage: `Password reset for ${user.fullName}`,
    validate: ({ newPassword, confirm }) => ({ confirm: newPassword !== confirm ? "Those passwords didn't match" : null }),
    onSubmit: ({ newPassword }) => UsersApi.resetPassword(user.userId, newPassword),
  });

const ACCOUNT_ACTIONS = {
  disable: {
    title: (u) => `Disable ${u.fullName}'s account?`,
    message: "They can't sign in, and any open session ends at their next click. Their records are kept. You can enable the account again later.",
    confirm: "Disable",
    danger: true,
    run: (u) => UsersApi.disable(u.userId),
    done: (u) => `${u.fullName}'s account is disabled`,
  },
  enable: {
    title: (u) => `Enable ${u.fullName}'s account?`,
    message: "They can sign in again with their current password.",
    confirm: "Enable",
    run: (u) => UsersApi.enable(u.userId),
    done: (u) => `${u.fullName}'s account is enabled`,
  },
  unlock: {
    title: (u) => `Unlock ${u.fullName}'s account?`,
    message: "It was locked after too many wrong passwords. Only unlock it if you've confirmed it was them.",
    confirm: "Unlock",
    run: (u) => UsersApi.unlock(u.userId),
    done: (u) => `${u.fullName}'s account is unlocked`,
  },
};

async function accountAction(user, kind) {
  const a = ACCOUNT_ACTIONS[kind];
  if (!(await confirmDialog({ title: a.title(user), message: a.message, confirmLabel: a.confirm, danger: a.danger }))) return false;
  try {
    await a.run(user);
    toast(a.done(user));
    return true;
  } catch (err) {
    toastError(err);
    return false;
  }
}

async function grantRequest(user, role) {
  const ok = await confirmDialog({
    title: `Give ${user.fullName} ${ROLE_LABELS[role]} access?`,
    message:
      role === "DEPT_SUPERVISOR"
        ? "They get supervisor permissions on their next request. Also link their employee record so they have a team to manage."
        : "They get the permissions of this role on their next request. Only approve requests you've confirmed with their manager.",
    confirmLabel: "Grant access",
    icon: "admin_panel_settings",
  });
  if (!ok) return false;
  try {
    await UsersApi.changeRole(user.userId, role);
    toast(`${user.fullName} now has ${ROLE_LABELS[role]} access`);
    return true;
  } catch (err) {
    toastError(err);
    return false;
  }
}

export default function UsersPage(ctx) {
  const action = takeAction(ctx);

  const create = async () => {
    if (await createUserDialog()) content.reload();
  };

  const content = asyncSection(
    async () => {
      const users = await UsersApi.list();
      // Open role requests and "forgot password" requests (see widgets/accounts.js)
      const { roles: pending, resets } = await openAccountRequests(users);
      return { users: users.sort((a, b) => a.fullName.localeCompare(b.fullName)), pending, resets };
    },
    ({ users, pending, resets }, reload) =>
      dataTable({
        rows: users,
        // ?filter=... from links on the home page and in notifications
        activeFilter: ["requests", "resets", "unlinked", "disabled", "locked"].includes(ctx.query.filter) ? ctx.query.filter : undefined,
        searchPlaceholder: "Search by name, email or role",
        searchText: (u) => `${u.fullName} ${u.email} ${ROLE_LABELS[u.role] ?? u.role}`,
        filters: [
          { id: "all", label: "All" },
          { id: "requests", label: pending.size ? `Access requests (${pending.size})` : "Access requests", predicate: (u) => pending.has(u.userId) },
          { id: "resets", label: resets.size ? `Password resets (${resets.size})` : "Password resets", predicate: (u) => resets.has(u.userId) },
          { id: "unlinked", label: "Not linked", predicate: (u) => u.employeeId == null },
          { id: "disabled", label: "Disabled", predicate: (u) => !u.active },
          { id: "locked", label: "Locked", predicate: (u) => u.locked },
          { id: "staff", label: "Employees & supervisors", predicate: (u) => ["EMPLOYEE", "DEPT_SUPERVISOR"].includes(u.role) },
          { id: "managers", label: "Managers & admins", predicate: (u) => !["EMPLOYEE", "DEPT_SUPERVISOR"].includes(u.role) },
        ],
        columns: [
          { key: "fullName", label: "User", render: (u) => personCell(u.fullName, u.email) },
          {
            key: "role",
            label: "Role",
            render: (u) =>
              h(
                "div",
                { class: "row row--wrap", style: { "--gap": "6px" } },
                h("span", { class: "status status--info" }, ROLE_LABELS[u.role] ?? u.role),
                pending.has(u.userId) && h("span", { class: "status status--warning" }, `Requested ${ROLE_LABELS[pending.get(u.userId)]}`)
              ),
            sortValue: (u) => ROLE_LABELS[u.role],
          },
          {
            key: "active",
            label: "Status",
            render: (u) =>
              h(
                "div",
                { class: "row row--wrap", style: { "--gap": "6px" } },
                !u.active
                  ? h("span", { class: "status status--error" }, "Disabled")
                  : u.locked
                    ? h("span", { class: "status status--warning", title: `Until ${fmtDateTime(u.lockedUntil)}` }, "Locked")
                    : h("span", { class: "status status--success" }, "Active"),
                u.mustChangePassword && h("span", { class: "status status--neutral" }, "Must change password"),
                resets.has(u.userId) && h("span", { class: "status status--warning", title: `Requested ${fmtDateTime(resets.get(u.userId))}` }, "Forgot password")
              ),
            sortValue: (u) => (!u.active ? 0 : u.locked ? 1 : 2),
          },
          { key: "employeeId", label: "Employee record", render: (u) => (u.employeeId != null ? `#${u.employeeId}` : h("span", { class: "status status--neutral" }, "Not linked")), sortValue: (u) => u.employeeId ?? -1 },
          { key: "phoneNumber", label: "Phone", render: (u) => u.phoneNumber || "—" },
          { key: "createdAt", label: "Created", render: (u) => fmtDate(u.createdAt) },
          {
            key: "actions",
            render: (u) => [
              pending.has(u.userId) &&
                button({ label: "Grant", variant: "tonal", size: "sm", onClick: async () => (await grantRequest(u, pending.get(u.userId))) && reload() }),
              resets.has(u.userId) &&
                button({ label: "Reset", variant: "tonal", size: "sm", onClick: async () => (await passwordDialog(u)) && reload() }),
              iconButton({
                icon: "more_vert",
                label: `Manage ${u.fullName}`,
                size: "sm",
                onClick: (event) =>
                  openMenu(event.currentTarget, {
                    items: [
                      // The API refuses changing your own role (it could lock every admin out)
                      u.userId !== ctx.user.userId && { label: "Change role", icon: "badge", onClick: async () => (await roleDialog(u)) && reload() },
                      { label: u.employeeId != null ? "Change employee link" : "Link employee record", icon: "link", onClick: async () => (await linkDialog(u)) && reload() },
                      { divider: true },
                      u.userId !== ctx.user.userId && { label: "Reset password", icon: "password", onClick: async () => (await passwordDialog(u)) && reload() },
                      u.locked && { label: "Unlock", icon: "lock_open", onClick: async () => (await accountAction(u, "unlock")) && reload() },
                      u.userId !== ctx.user.userId &&
                        (u.active
                          ? { label: "Disable account", icon: "block", onClick: async () => (await accountAction(u, "disable")) && reload() }
                          : { label: "Enable account", icon: "check_circle", onClick: async () => (await accountAction(u, "enable")) && reload() }),
                    ],
                  }),
              }),
            ],
          },
        ],
        empty: { icon: "manage_accounts", title: "No user accounts" },
      })
  );

  if (action === "new") setTimeout(create);

  return page(
    pageHeader({
      title: "Users & access",
      subtitle: "Accounts, roles, access and password-reset requests, locks and links to employee records",
      actions: button({ label: "New user", icon: "person_add", onClick: create }),
    }),
    content
  );
}
