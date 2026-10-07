// Self-registration for existing employees. The work email and NIC must match the
// employee record HR created; the new account is linked to that record straight away,
// so the employee can check in, apply for leave and see payslips immediately.

import { h, icon } from "../../core/dom.js";
import { AuthApi } from "../../services/api.js";
import { button, withBusy } from "../../components/button.js";
import { field, readForm, setFieldError, validateForm } from "../../components/fields.js";
import { toast } from "../../components/snackbar.js";
import { v } from "../../core/validators.js";

export const REGISTER_FIELDS = [
  { name: "email", label: "Work email", type: "email", required: true, maxLength: 120, normalize: "lower", autocomplete: "email", span: "all", help: "The email HR has on your employee record", validate: v.email },
  { name: "nic", label: "NIC", required: true, maxLength: 12, normalize: "upper", help: "Must match your employee record", validate: v.nic },
  { name: "phoneNumber", label: "Phone (optional)", type: "tel", maxLength: 20, normalize: "phone", autocomplete: "tel", help: "e.g. 0771234567", validate: v.phone },
  { name: "password", label: "Password", type: "password", required: true, minLength: 8, maxLength: 72, autocomplete: "new-password", help: "8+ characters with a letter and a number", validate: v.password },
  { name: "confirm", label: "Confirm", type: "password", required: true, maxLength: 72, autocomplete: "new-password" },
];

// Shared by this page and the "request management access" form on the sign-in page
export function registrationProblems(values) {
  if (values.password !== values.confirm) return { confirm: "Those passwords didn't match" };
  const localPart = values.email.split("@")[0];
  if (localPart.length >= 4 && values.password.toLowerCase().includes(localPart)) return { password: "The password must not contain your email name" };
  return null;
}

export default function RegisterPage(ctx) {
  const fields = REGISTER_FIELDS.map((def) => field(def));
  const byName = Object.fromEntries(REGISTER_FIELDS.map((def, i) => [def.name, fields[i]]));

  const errorBanner = h("div", { class: "banner banner--error span-all", role: "alert", hidden: true });
  const submit = button({ label: "Create account", type: "submit" });

  const form = h(
    "form",
    {
      class: "auth__form",
      novalidate: true,
      onSubmit: async (event) => {
        event.preventDefault();
        errorBanner.hidden = true;
        if (!validateForm(form)) return;

        const values = readForm(form, REGISTER_FIELDS);
        const problems = registrationProblems(values);
        if (problems) {
          const [name, message] = Object.entries(problems)[0];
          setFieldError(byName[name], message);
          byName[name].control.focus();
          return;
        }

        const { confirm, ...body } = values;
        try {
          await withBusy(submit, async () => {
            await AuthApi.register(body);
            const user = await AuthApi.login(body.email, body.password);
            toast("Account created and linked to your employee record.");
            ctx.completeSignIn(user);
          });
        } catch (err) {
          errorBanner.replaceChildren(icon("error"), h("span", null, err.message));
          errorBanner.hidden = false;
        }
      },
    },
    h("div", { class: "form-grid" }, errorBanner, fields),
    h("div", { class: "auth__actions" }, button({ label: "Sign in instead", variant: "text", href: "#/login" }), submit)
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
          h("h1", { class: "auth__title" }, "Create your account"),
          h("p", { class: "auth__lead" }, "For LankaTech employees."),
          h(
            "p",
            { class: "body-medium muted pretty" },
            "Use the work email and NIC on your employee record. Your account is linked to it straight away, so you can check in, apply for leave and view payslips. If it doesn't match, ask HR to check your record."
          )
        ),
        form
      ),
      h("div", { class: "auth__footer" }, h("span", null, "LankaTech Services (Pvt) Ltd"), h("span", null, "Employee Management System"))
    )
  );
}
