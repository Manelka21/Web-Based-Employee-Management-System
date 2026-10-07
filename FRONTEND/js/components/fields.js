// Outlined text fields + declarative form helpers.
//
// A field definition:
//   { name, label, type: "text"|"email"|"password"|"number"|"date"|"time"|"select"|"textarea",
//     value, required, options: [{ value, label }] | ["A", "B"], min, max, step, maxLength,
//     help, span: "all", autocomplete, disabled, emptyLabel, parse: "int"|"number",
//     normalize: "phone"|"upper"|"lower", validate: (value, form) => message | null }

import { h } from "../core/dom.js";
import { NORMALIZERS } from "../core/validators.js";

let uid = 0;
const FLOATING = new Set(["date", "time", "month", "select", "datetime-local"]);

function normaliseOptions(options = []) {
  return options.map((opt) => (typeof opt === "object" ? opt : { value: opt, label: opt }));
}

export function field(def) {
  const id = `f${++uid}`;
  const type = def.type || "text";
  const supportId = `${id}-support`;
  const common = {
    id,
    name: def.name,
    class: "field__control",
    required: def.required,
    disabled: def.disabled,
    placeholder: def.placeholder ?? " ",
    "aria-describedby": supportId,
  };

  let control;
  if (type === "select") {
    const options = normaliseOptions(typeof def.options === "function" ? def.options() : def.options);
    control = h(
      "select",
      common,
      !def.required || def.emptyLabel ? h("option", { value: "" }, def.emptyLabel ?? "None") : null,
      options.map((opt) =>
        h("option", { value: opt.value, selected: def.value != null && String(def.value) === String(opt.value) }, opt.label)
      )
    );
    if (def.required && !def.emptyLabel && def.value == null && options.length) control.value = String(options[0].value);
  } else if (type === "textarea") {
    control = h("textarea", { ...common, rows: def.rows || 4, maxLength: def.maxLength });
    control.value = def.value ?? "";
  } else {
    control = h("input", {
      ...common,
      type,
      min: def.min,
      max: def.max,
      step: def.step,
      maxLength: def.maxLength,
      minLength: def.minLength,
      autocomplete: def.autocomplete,
      inputmode: def.inputmode,
    });
    if (def.value != null) control.value = String(type === "time" ? String(def.value).slice(0, 5) : def.value);
  }

  const support = h("span", { class: "field__support", id: supportId, hidden: !def.help }, def.help ?? "");
  const wrapper = h(
    "div",
    {
      class: [
        "field",
        (FLOATING.has(type) || def.placeholder) && "field--float",
        def.leadingIcon && "field--leading",
        def.span === "all" && "span-all",
      ],
    },
    control,
    h("label", { class: ["field__label", def.required && "field-required"], for: id }, def.label),
    support
  );

  control.addEventListener("input", () => clearFieldError(wrapper, def.help));
  control.addEventListener("change", () => clearFieldError(wrapper, def.help));
  // Validate when the user leaves a field they typed in, so mistakes show early
  if (def.validate) {
    control.addEventListener("blur", () => {
      if (control.disabled || !control.value) return;
      const message = def.validate(fieldValue(def, control), control.form);
      if (message) setFieldError(wrapper, message);
    });
  }
  wrapper.control = control;
  wrapper.def = def;
  return wrapper;
}

// Trimmed + normalised string value of a control (what validators and readForm see)
function fieldValue(def, control) {
  let value = typeof control.value === "string" ? control.value.trim() : control.value;
  if (value && def.normalize && NORMALIZERS[def.normalize]) value = NORMALIZERS[def.normalize](value);
  return value;
}

function clearFieldError(wrapper, help) {
  if (!wrapper.classList.contains("is-invalid")) return;
  wrapper.classList.remove("is-invalid");
  const support = wrapper.querySelector(".field__support");
  support.textContent = help ?? "";
  support.hidden = !help;
}

export function setFieldError(wrapper, message) {
  wrapper.classList.add("is-invalid");
  const support = wrapper.querySelector(".field__support");
  support.textContent = message;
  support.hidden = false;
}

// Native constraint validation plus each field's own `validate` rule, surfaced in
// M3 style. Returns true when valid.
export function validateForm(form) {
  let firstInvalid = null;
  for (const wrapper of form.querySelectorAll(".field")) {
    const control = wrapper.control || wrapper.querySelector(".field__control");
    if (!control || control.disabled) continue;

    // Whitespace-only text passes the native `required` check, so catch it here
    const def = wrapper.def || {};
    const value = fieldValue(def, control);
    let message = null;
    if (!control.checkValidity()) message = control.validationMessage;
    else if (control.required && value === "") message = "Please fill in this field.";
    else if (def.validate && value !== "") message = def.validate(value, form);

    if (!message) continue;
    setFieldError(wrapper, message);
    firstInvalid ??= control;
  }
  firstInvalid?.focus();
  return !firstInvalid;
}

// Shows errors returned by a form-level check: { fieldName: "message" }
export function showFieldErrors(form, errors) {
  let first = null;
  for (const [name, message] of Object.entries(errors || {})) {
    if (!message) continue;
    const control = form.elements.namedItem(name);
    const wrapper = control?.closest(".field");
    if (!wrapper) continue;
    setFieldError(wrapper, message);
    first ??= control;
  }
  first?.focus();
  return !first;
}

// Reads values for the given definitions. Empty strings become null; numbers are parsed.
export function readForm(form, defs) {
  const values = {};
  for (const def of defs) {
    if (!def.name) continue;
    const control = form.elements.namedItem(def.name);
    if (!control) continue;
    let value = fieldValue(def, control);
    if (value === "") value = null;
    else if (def.parse === "int") value = parseInt(value, 10);
    else if (def.parse === "number" || def.type === "number") value = Number(value);
    values[def.name] = value;
  }
  return values;
}

// Compact pill controls for toolbars (filters above tables)
export function compactSelect({ label, options, value, emptyLabel, onChange }) {
  const select = h(
    "select",
    { class: "control-compact", "aria-label": label, onChange: (event) => onChange?.(event.target.value) },
    emptyLabel != null && h("option", { value: "" }, emptyLabel),
    normaliseOptions(options).map((opt) => h("option", { value: opt.value, selected: value != null && String(value) === String(opt.value) }, opt.label))
  );
  // <label>.control is a read-only built-in (assigning it throws in module code);
  // it already returns the wrapped <select>, so callers can read wrapper.control.
  return h("label", { class: "toolbar-label" }, h("span", null, label), select);
}

export function compactInput({ label, type = "text", value, min, max, onChange }) {
  const input = h("input", { class: "control-compact", type, "aria-label": label, value, min, max, onChange: (event) => onChange?.(event.target.value) });
  // wrapper.control is the native <label>.control getter, which returns the wrapped <input>
  return h("label", { class: "toolbar-label" }, h("span", null, label), input);
}

export const optionsFrom = (list, valueKey, labelFn) =>
  list.map((item) => ({ value: item[valueKey], label: typeof labelFn === "function" ? labelFn(item) : item[labelFn] }));

export const enumOptions = (values, labeler) => values.map((v) => ({ value: v, label: labeler ? labeler(v) : v }));
