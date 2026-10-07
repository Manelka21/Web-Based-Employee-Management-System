// Full-screen "choose a new password" page. Users land here after an IT administrator
// reset their password or created their account: the API refuses everything else
// (403 PASSWORD_CHANGE_REQUIRED) until they pick their own password.

import { h, icon } from "../../core/dom.js";
import { session } from "../../core/session.js";
import { v } from "../../core/validators.js";
import { AuthApi } from "../../services/api.js";
import { button, withBusy } from "../../components/button.js";
import { field, readForm, setFieldError, validateForm } from "../../components/fields.js";
import { toast } from "../../components/snackbar.js";

export const PASSWORD_FIELDS = [
  { name: "currentPassword", label: "Current (temporary) password", type: "password", required: true, maxLength: 200, autocomplete: "current-password", span: "all" },
  { name: "newPassword", label: "New password", type: "password", required: true, minLength: 8, maxLength: 72, autocomplete: "new-password", help: "8+ characters with a letter and a number", validate: v.password },
  { name: "confirm", label: "Confirm new password", type: "password", required: true, maxLength: 72, autocomplete: "new-password" },
];

// Cross-field rules shared with the profile dialog
export function passwordRules({ currentPassword, newPassword, confirm }) {
  return {
    newPassword: newPassword && newPassword === currentPassword ? "Choose a password different from the current one" : null,
    confirm: newPassword !== confirm ? "Those passwords didn't match" : null,
  };
}

export default function ChangePasswordPage(ctx) {
  const fields = PASSWORD_FIELDS.map((def) => field(def));
  const byName = Object.fromEntries(PASSWORD_FIELDS.map((def, i) => [def.name, fields[i]]));
  const errorBanner = h("div", { class: "banner banner--error span-all", role: "alert", hidden: true });
  const submit = button({ label: "Save password", type: "submit" });

  const form = h(
    "form",
    {
      class: "auth__form",
      novalidate: true,
      onSubmit: async (event) => {
        event.preventDefault();
        errorBanner.hidden = true;
        if (!validateForm(form)) return;
        const values = readForm(form, PASSWORD_FIELDS);
        const problems = Object.entries(passwordRules(values)).filter(([, message]) => message);
        if (problems.length) {
          problems.forEach(([name, message]) => setFieldError(byName[name], message));
          byName[problems[0][0]].control.focus();
          return;
        }
        try {
          await withBusy(submit, () => AuthApi.changePassword(values.currentPassword, values.newPassword));
          session.set({ user: { ...session.user, mustChangePassword: false } });
          toast("Password saved. Welcome!");
          ctx.navigate("/");
        } catch (err) {
          errorBanner.replaceChildren(icon("error"), h("span", null, err.message));
          errorBanner.hidden = false;
        }
      },
    },
    h("div", { class: "form-grid" }, errorBanner, fields),
    h("div", { class: "auth__actions" }, h("span"), submit)
  );

  requestAnimationFrame(() => fields[0].control.focus());

  return h(
    "div",
    { class: "auth" },
    h(
      "div",
      { class: "auth__wrap" },
      h(
        "div",
        { class: "auth__card" },
        h(
          "div",
          { class: "auth__intro" },
          h("img", { class: "auth__logo", src: "assets/logo.svg", alt: "LankaTech" }),
          h("h1", { class: "auth__title" }, "Choose a new password"),
          h("p", { class: "auth__lead" }, `Signed in as ${ctx.user?.email ?? ""}`),
          h("p", { class: "body-medium muted pretty" }, "Your password was set by an IT administrator. Choose your own before you continue.")
        ),
        form
      )
    )
  );
}
