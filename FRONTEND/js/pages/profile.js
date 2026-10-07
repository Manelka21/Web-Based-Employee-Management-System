import { h } from "../core/dom.js";
import { fmtDate, fmtDateTime, fullName, humanize } from "../core/format.js";
import { isLinked, ROLE_LABELS } from "../core/permissions.js";
import { AuthApi, EmployeesApi } from "../services/api.js";
import { PASSWORD_FIELDS, passwordRules } from "./auth/changePassword.js";
import { v } from "../core/validators.js";
import { departmentLabel, lookups, positionLabel } from "../services/lookups.js";
import { asyncSection } from "../components/feedback.js";
import { avatar, banner, card, kv, page, pageHeader } from "../components/page.js";
import { button } from "../components/button.js";
import { formDialog } from "../components/dialog.js";
import { statusBadge } from "../components/status.js";

export default function ProfilePage(ctx) {
  const { user } = ctx;

  const head = h(
    "section",
    { class: "profile-head" },
    avatar(user.fullName, { size: "xl" }),
    h(
      "div",
      { class: "profile-head__body" },
      h("h1", { class: "headline-medium" }, user.fullName),
      h("p", { class: "body-large muted" }, user.email),
      h("div", { class: "row row--wrap", style: { marginTop: "10px", "--gap": "8px" } }, h("span", { class: "status status--info" }, ROLE_LABELS[user.role] || user.role))
    )
  );

  const changePassword = () =>
    formDialog({
      title: "Change password",
      fields: PASSWORD_FIELDS.map((f) => (f.name === "currentPassword" ? { ...f, label: "Current password" } : f)),
      submitLabel: "Change password",
      successMessage: "Password changed",
      validate: passwordRules,
      onSubmit: ({ currentPassword, newPassword }) => AuthApi.changePassword(currentPassword, newPassword),
    });

  const account = card(
    { title: "Account", actions: button({ label: "Change password", icon: "password", variant: "tonal", size: "sm", onClick: changePassword }) },
    kv([
      ["Email", user.email],
      ["Phone", user.phoneNumber || "—"],
      ["Role", ROLE_LABELS[user.role] || user.role],
      ["User ID", `#${user.userId}`],
      ["Member since", fmtDateTime(user.createdAt)],
      ["Employee record", isLinked(user) ? `#${user.employeeId}` : "Not linked"],
    ])
  );

  const employee = isLinked(user)
    ? asyncSection(
        async () => {
          const [me, maps] = await Promise.all([EmployeesApi.me(), lookups.maps()]);
          return { me, maps };
        },
        ({ me, maps }, reload) =>
          card(
            {
              title: "Employee record",
              subtitle: "Only your phone number and address can be changed here. Contact HR for anything else.",
              actions: button({
                label: "Edit contact details",
                icon: "edit",
                variant: "tonal",
                size: "sm",
                onClick: async () => {
                  const saved = await formDialog({
                    title: "Contact details",
                    fields: [
                      { name: "phone", label: "Phone", type: "tel", value: me.phone, maxLength: 20, normalize: "phone", span: "all", help: "e.g. 0771234567", validate: v.phone },
                      { name: "address", label: "Address", type: "textarea", rows: 3, value: me.address, maxLength: 255, span: "all" },
                    ],
                    successMessage: "Contact details updated",
                    // The API ignores null fields, so a cleared value is sent as ""
                    onSubmit: (values) => EmployeesApi.updateMe({ phone: values.phone ?? "", address: values.address ?? "" }),
                  });
                  if (saved) {
                    lookups.invalidate("myEmployee", "employees", "team");
                    reload();
                  }
                },
              }),
            },
            kv([
              ["Name", fullName(me)],
              ["Status", statusBadge(me.status)],
              ["Department", departmentLabel(maps.departments, me.departmentId)],
              ["Position", positionLabel(maps.positions, me.positionId)],
              ["Hire date", fmtDate(me.hireDate)],
              ["NIC", me.nic],
              ["Work email", me.email],
              ["Phone", me.phone || "—"],
              ["Gender", me.gender ? humanize(me.gender) : "Not recorded"],
              ["Address", me.address || "—"],
            ])
          )
      )
    : banner(
        "info",
        "link_off",
        "Your account isn't linked to an employee record, so self-service features like leave, attendance and payslips aren't available. Ask your IT administrator to link it."
      );

  return page(pageHeader({ title: "My profile" }), h("div", { class: "stack", style: { "--gap": "16px" } }, head, account, employee));
}
