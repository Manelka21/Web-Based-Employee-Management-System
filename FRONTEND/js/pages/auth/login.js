// Sign-in page. Everything happens on this one screen:
//   • Employee tab            — sign in with an EMPLOYEE account (or create one on #/register)
//   • Admin & management tab  — sign in as HR, supervisor, payroll, director or IT admin,
//                               or request a management account
//
// Requesting an account calls POST /api/auth/register with the requested role. The backend
// always creates an EMPLOYEE account and records the request for the IT administrators, who
// grant the role in Users & access. Nobody can give themselves elevated access.

import { h, icon } from "../../core/dom.js";
import { setQuery } from "../../core/router.js";
import { AuthApi } from "../../services/api.js";
import { button, iconButton, withBusy } from "../../components/button.js";
import { field, readForm, setFieldError, validateForm } from "../../components/fields.js";
import { ROLE_LABELS, ROLES } from "../../core/permissions.js";
import { v } from "../../core/validators.js";
import { formDialog } from "../../components/dialog.js";
import { toast } from "../../components/snackbar.js";
import { REGISTER_FIELDS, registrationProblems } from "./register.js";

const MANAGEMENT_ROLES = [ROLES.HR, ROLES.SUPERVISOR, ROLES.PAYROLL, ROLES.DIRECTOR, ROLES.ADMIN];

const PORTALS = {
  employee: {
    tab: "Employee",
    icon: "badge",
    title: "Sign in",
    lead: "Use your LankaTech work account",
    roles: [ROLES.EMPLOYEE],
  },
  staff: {
    tab: "Admin & management",
    icon: "admin_panel_settings",
    title: "Management sign-in",
    lead: "For HR, supervisors, payroll, directors and IT administrators",
    roles: MANAGEMENT_ROLES,
  },
};

// Seed accounts from BACKEND/src/main/resources/schema.sql (password: password123)
const DEMO_ACCOUNTS = [
  { email: "employee@lankatech.lk", role: ROLES.EMPLOYEE },
  { email: "hr@lankatech.lk", role: ROLES.HR },
  { email: "supervisor@lankatech.lk", role: ROLES.SUPERVISOR },
  { email: "payroll@lankatech.lk", role: ROLES.PAYROLL },
  { email: "director@lankatech.lk", role: ROLES.DIRECTOR },
  { email: "admin@lankatech.lk", role: ROLES.ADMIN },
];

export default function LoginPage(ctx) {
  let mode = ctx.query.portal === "staff" || ctx.query.view === "request" ? "staff" : "employee";

  // ------------------------------------------------------------------ Intro (left column)
  const portalIcon = h("span", { class: "auth__portal-icon" });
  const title = h("h1", { class: "auth__title" });
  const lead = h("p", { class: "auth__lead" });
  const panel = h("div", { class: "auth__panel" });

  function setIntro(iconName, titleText, leadText) {
    portalIcon.replaceChildren(iconName ? icon(iconName) : "");
    portalIcon.hidden = !iconName;
    title.textContent = titleText;
    lead.textContent = leadText;
  }

  function makeBanner() {
    const el = h("div", { class: "banner", role: "alert", hidden: true });
    el.show = (tone, iconName, message, action) => {
      el.className = `banner banner--${tone}`;
      el.replaceChildren(icon(iconName), h("div", { class: "stack", style: { "--gap": "8px", flex: "1" } }, h("span", null, message), action));
      el.hidden = false;
    };
    return el;
  }

  // ------------------------------------------------------------------ Sign-in form
  const emailField = field({ name: "email", label: "Email", type: "email", required: true, maxLength: 120, autocomplete: "username", validate: v.email });
  const passwordField = field({ name: "password", label: "Password", type: "password", required: true, maxLength: 200, autocomplete: "current-password" });
  addPasswordToggle(passwordField);

  const signInBanner = makeBanner();
  const footerAction = h("div");
  const demoChips = h("div", { class: "chip-set" });

  const segmentButtons = Object.entries(PORTALS).map(([id, portal]) =>
    h("button", { type: "button", class: "state", dataset: { portal: id }, onClick: () => setMode(id, { focus: true }) }, icon(portal.icon), portal.tab)
  );
  const segmented = h("div", { class: "segmented auth__portals", role: "group", "aria-label": "Choose sign-in portal" }, segmentButtons);

  const submit = button({ label: "Sign in", type: "submit" });

  const signInForm = h(
    "form",
    {
      class: "auth__form",
      novalidate: true,
      onSubmit: async (event) => {
        event.preventDefault();
        signInBanner.hidden = true;
        if (!validateForm(signInForm)) return;

        const email = emailField.control.value.trim();
        const password = passwordField.control.value;
        const portal = PORTALS[mode];

        try {
          await withBusy(submit, async () => {
            const user = await AuthApi.login(email, password);

            // Right credentials, wrong portal: end the session and point to the other one
            if (!portal.roles.includes(user.role)) {
              await AuthApi.logout().catch(() => {});
              const other = mode === "staff" ? "employee" : "staff";
              signInBanner.show(
                "warning",
                "swap_horiz",
                mode === "staff"
                  ? "This account has employee access only. If you requested management access, an IT administrator still needs to approve it. Until then, use the Employee sign-in."
                  : `This is a ${ROLE_LABELS[user.role] || "management"} account. Use the Admin & management sign-in instead.`,
                h("div", null, button({ label: `Go to ${PORTALS[other].tab} sign-in`, variant: "text", size: "sm", icon: "arrow_forward", iconEnd: true, onClick: () => setMode(other, { email }) }))
              );
              return;
            }

            ctx.completeSignIn(user, ctx.query.next);
          });
        } catch (err) {
          if (err.status === 401) {
            setFieldError(passwordField, "Wrong email or password. Try again.");
            passwordField.control.select();
          } else if (err.status === 403) {
            // Account disabled by the IT admin, or its employee record was deactivated
            signInBanner.show("error", "block", err.message);
          } else if (err.status === 423) {
            // Locked after too many wrong passwords
            signInBanner.show("error", "lock_clock", err.message, h("div", null, forgotButton()));
          } else {
            signInBanner.show("error", "error", err.message);
          }
        }
      },
    },
    segmented,
    signInBanner,
    emailField,
    passwordField,
    h("div", { style: { marginTop: "-8px" } }, forgotButton()),
    h("div", { class: "auth__actions" }, footerAction, submit),
    window.EMS_CONFIG?.showDemoAccounts &&
      h("details", { class: "demo-accounts" }, h("summary", null, icon("science", { size: "sm" }), "Use a demo account"), demoChips)
  );

  function setMode(next, { focus = false, email } = {}) {
    mode = next;
    const portal = PORTALS[mode];
    signInBanner.hidden = true;
    setQuery({ portal: mode === "staff" ? "staff" : null, view: null });

    segmentButtons.forEach((btn) => {
      const on = btn.dataset.portal === mode;
      btn.classList.toggle("is-selected", on);
      btn.setAttribute("aria-pressed", String(on));
    });

    setIntro(mode === "staff" ? portal.icon : null, portal.title, portal.lead);

    footerAction.replaceChildren(
      mode === "employee"
        ? button({ label: "Create account", variant: "text", href: "#/register" })
        : button({ label: "Request an account", variant: "text", icon: "person_add", onClick: showRequest })
    );

    demoChips.replaceChildren(
      ...DEMO_ACCOUNTS.filter((acc) => portal.roles.includes(acc.role)).map((acc) =>
        h(
          "button",
          {
            type: "button",
            class: "chip state",
            title: acc.email,
            onClick: () => {
              emailField.control.value = acc.email;
              passwordField.control.value = "password123";
              signInForm.requestSubmit();
            },
          },
          ROLE_LABELS[acc.role]
        )
      )
    );

    panel.replaceChildren(signInForm);
    if (email) emailField.control.value = email;
    if (focus || email) (email ? passwordField : emailField).control.focus();
  }

  // ------------------------------------------------------------------ Forgot password
  // There's no email service for reset links, so the request goes to the IT admins,
  // who set a temporary password; the user then has to choose a new one at sign-in.
  function forgotButton() {
    return button({
      label: "Forgot password?",
      variant: "text",
      size: "sm",
      onClick: () =>
        formDialog({
          title: "Forgot your password?",
          intro: "We'll ask the IT administrators to reset it. They'll give you a temporary password, and you'll choose a new one when you sign in.",
          fields: [{ name: "email", label: "Work email", type: "email", required: true, maxLength: 120, normalize: "lower", value: emailField.control.value.trim() || null, span: "all", validate: v.email }],
          submitLabel: "Send request",
          onSubmit: async ({ email }) => {
            const res = await AuthApi.forgotPassword(email);
            toast(res.message, { duration: 8000 });
            return true;
          },
        }),
    });
  }

  // ------------------------------------------------------------------ Request-an-account form
  function showRequest() {
    setQuery({ portal: "staff", view: "request" });
    setIntro(
      "person_add",
      "Request management access",
      "Your account starts with employee access. An IT administrator reviews the role you ask for and upgrades your account."
    );

    // Same identity check as self-registration (email + NIC on the employee record), plus the role wanted
    const defs = [
      ...REGISTER_FIELDS.slice(0, 2),
      { name: "role", label: "Access you need", type: "select", required: true, emptyLabel: "Choose a role", options: MANAGEMENT_ROLES.map((r) => ({ value: r, label: ROLE_LABELS[r] })) },
      ...REGISTER_FIELDS.slice(2),
    ];
    const fields = defs.map((def) => field(def));
    const byName = Object.fromEntries(defs.map((def, i) => [def.name, fields[i]]));
    const banner = makeBanner();
    const send = button({ label: "Send request", type: "submit" });

    const form = h(
      "form",
      {
        class: "auth__form",
        novalidate: true,
        onSubmit: async (event) => {
          event.preventDefault();
          banner.hidden = true;
          if (!validateForm(form)) return;

          const values = readForm(form, defs);
          const problems = registrationProblems(values);
          if (problems) {
            const [name, message] = Object.entries(problems)[0];
            setFieldError(byName[name], message);
            byName[name].control.focus();
            return;
          }

          const { confirm, ...body } = values;
          try {
            await withBusy(send, () => AuthApi.register(body));
            showSuccess(body.email, body.role);
          } catch (err) {
            if (err.status === 409) {
              setFieldError(byName.email, "An account with this email already exists. Sign in instead, or ask your IT administrator to change its role.");
              byName.email.control.focus();
            } else {
              banner.show("error", "error", err.message);
            }
          }
        },
      },
      h("div", { class: "form-grid" }, h("div", { class: "span-all" }, banner), fields),
      h("div", { class: "auth__actions" }, button({ label: "Back to sign in", variant: "text", icon: "arrow_back", onClick: () => setMode("staff") }), send)
    );

    panel.replaceChildren(form);
    requestAnimationFrame(() => fields[0].control.focus());
  }

  function showSuccess(email, role) {
    setQuery({ portal: "staff", view: null });
    setIntro("mark_email_read", "Request sent", `The IT administrators have been notified that you need ${ROLE_LABELS[role]} access.`);

    const signInAsEmployee = button({ label: "Sign in with employee access", onClick: () => setMode("employee", { email }) });
    panel.replaceChildren(
      h(
        "div",
        { class: "auth__form" },
        h(
          "div",
          { class: "banner banner--success", role: "status" },
          icon("check_circle"),
          h("div", null, h("strong", null, "Your account is ready. "), `You can sign in as ${email} on the Employee tab straight away.`)
        ),
        h(
          "p",
          { class: "body-medium muted pretty" },
          `Once your ${ROLE_LABELS[role]} access is approved, sign in on the Admin & management tab with the same email and password.`
        ),
        h("div", { class: "auth__actions" }, button({ label: "Back to management sign-in", variant: "text", onClick: () => setMode("staff") }), signInAsEmployee)
      )
    );
    signInAsEmployee.focus();
  }

  // ------------------------------------------------------------------ Mount
  setMode(mode);
  if (ctx.query.view === "request") showRequest();
  else requestAnimationFrame(() => emailField.control.focus());

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
          h("div", { class: "row", style: { "--gap": "12px" } }, h("img", { class: "auth__logo", src: "assets/logo.svg", alt: "LankaTech" }), portalIcon),
          title,
          lead
        ),
        panel
      ),
      h("div", { class: "auth__footer" }, h("span", null, "LankaTech Services (Pvt) Ltd"), h("span", null, "Employee Management System"))
    )
  );
}

function addPasswordToggle(passwordField) {
  const toggle = iconButton({
    icon: "visibility",
    label: "Show password",
    onClick: () => {
      const input = passwordField.control;
      const show = input.type === "password";
      input.type = show ? "text" : "password";
      toggle.querySelector(".icon").textContent = show ? "visibility_off" : "visibility";
      toggle.setAttribute("aria-label", show ? "Hide password" : "Show password");
    },
  });
  passwordField.append(h("span", { class: "field__trailing" }, toggle));
  passwordField.control.style.paddingRight = "52px";
}
