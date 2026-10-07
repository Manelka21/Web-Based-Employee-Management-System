// Modal dialogs built on <dialog> (focus trap, Esc and inert background for free).

import { h, icon } from "../core/dom.js";
import { button, withBusy } from "./button.js";
import { field, readForm, showFieldErrors, validateForm } from "./fields.js";
import { toast } from "./snackbar.js";

let dialogSeq = 0;

export function openDialog({ title, icon: iconName, content, actions, wide = false, dismissible = true } = {}) {
  let resolveResult;
  const result = new Promise((resolve) => (resolveResult = resolve));
  const titleId = `dlg-title-${++dialogSeq}`;

  const dialog = h("dialog", { class: ["dialog", wide && "dialog--wide"], "aria-labelledby": titleId });

  let closed = false;
  const close = (value) => {
    if (closed) return;
    closed = true;
    dialog.classList.add("is-closing");
    const finish = () => {
      dialog.close();
      dialog.remove();
      resolveResult(value);
    };
    dialog.addEventListener("animationend", finish, { once: true });
    setTimeout(finish, 250);
  };

  const header = h(
    "div",
    { class: ["dialog__header", iconName && "dialog__header--icon"] },
    iconName && icon(iconName),
    h("h2", { class: "dialog__title", id: titleId }, title)
  );
  const body = h("div", { class: "dialog__body" }, typeof content === "function" ? content(close) : content);
  const footer = h("div", { class: "dialog__actions" }, typeof actions === "function" ? actions(close) : actions);

  dialog.append(header, body, footer);
  dialog.addEventListener("cancel", (event) => {
    event.preventDefault();
    if (dismissible) close(undefined);
  });
  dialog.addEventListener("click", (event) => {
    if (dismissible && event.target === dialog) close(undefined);
  });

  document.body.append(dialog);
  dialog.showModal();
  return { dialog, close, result, body, footer };
}

export function confirmDialog({ title, message, confirmLabel = "Confirm", cancelLabel = "Cancel", danger = false, icon: iconName } = {}) {
  const { result } = openDialog({
    title,
    icon: iconName,
    content: h("p", { class: "body-medium pretty" }, message),
    actions: (close) => [
      button({ label: cancelLabel, variant: "text", onClick: () => close(false) }),
      button({ label: confirmLabel, variant: danger ? "danger-text" : "text", onClick: () => close(true) }),
    ],
  });
  return result.then(Boolean);
}

/**
 * A dialog containing a form. onSubmit(values) may return a value (passed back as the
 * dialog result) or throw — the error message is shown inside the dialog.
 */
export function formDialog({
  title,
  intro,
  fields: defs,
  submitLabel = "Save",
  successMessage,
  wide = false,
  extra,
  onMount,
  validate,
  onSubmit,
}) {
  const errorBanner = h("div", { class: "banner banner--error span-all", role: "alert", hidden: true });
  const form = h(
    "form",
    { class: "form-grid", novalidate: true },
    intro && h("p", { class: "span-all body-medium muted pretty" }, intro),
    errorBanner,
    defs.map((def) => field(def)),
    extra
  );

  const submitBtn = button({ label: submitLabel, variant: "filled", type: "submit" });
  form.id = `dlg-form-${dialogSeq + 1}`;
  submitBtn.setAttribute("form", form.id);

  const handle = openDialog({
    title,
    wide,
    content: form,
    actions: (close) => [button({ label: "Cancel", variant: "text", onClick: () => close(undefined) }), submitBtn],
  });

  // validate(values) may return { fieldName: "message" } for cross-field rules
  // (end date after start date, passwords match, ...). Nothing is sent while any fail.
  let submitting = false;
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (submitting) return;
    errorBanner.hidden = true;
    if (!validateForm(form)) return;

    const values = readForm(form, defs);
    if (validate && !showFieldErrors(form, validate(values))) return;

    submitting = true;
    try {
      const outcome = await withBusy(submitBtn, () => onSubmit(values, form));
      if (successMessage) toast(typeof successMessage === "function" ? successMessage(outcome) : successMessage);
      handle.close(outcome ?? true);
    } catch (err) {
      errorBanner.replaceChildren(icon("error"), h("span", null, err.message || "Something went wrong"));
      errorBanner.hidden = false;
      errorBanner.scrollIntoView({ block: "nearest", behavior: "smooth" });
    } finally {
      submitting = false;
    }
  });

  onMount?.(form);
  requestAnimationFrame(() => form.querySelector(".field__control:not([disabled])")?.focus());
  return handle.result;
}
