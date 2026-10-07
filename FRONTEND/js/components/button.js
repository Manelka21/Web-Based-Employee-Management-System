import { h, icon } from "../core/dom.js";

export function button({
  label,
  icon: iconName,
  variant = "filled",
  size,
  onClick,
  type = "button",
  disabled,
  title,
  iconEnd = false,
  cls,
  href,
} = {}) {
  const classes = ["btn", "state", `btn--${variant}`, size && `btn--${size}`, iconEnd && "btn--icon-end", cls];
  const children = [
    !iconEnd && iconName && icon(iconName),
    h("span", null, label),
    iconEnd && iconName && icon(iconName),
  ];
  if (href) return h("a", { class: classes, href, title }, children);
  return h("button", { type, class: classes, onClick, disabled, title }, children);
}

export function iconButton({ icon: iconName, label, onClick, variant, size, fill = false, cls, disabled } = {}) {
  return h(
    "button",
    {
      type: "button",
      class: ["icon-btn", "state", variant && `icon-btn--${variant}`, size && `icon-btn--${size}`, cls],
      "aria-label": label,
      title: label,
      onClick,
      disabled,
    },
    icon(iconName, { fill })
  );
}

// Shows a spinner in the button while the async task runs, and disables it so a
// double click can't send the same request twice (duplicate records).
export async function withBusy(btn, task) {
  if (!btn) return task();
  if (btn.classList.contains("is-busy")) return undefined;
  const spinner = h("span", { class: "spinner", "aria-hidden": "true" });
  const wasDisabled = btn.disabled;
  btn.classList.add("is-busy");
  btn.setAttribute("aria-busy", "true");
  btn.disabled = true;
  btn.append(spinner);
  try {
    return await task();
  } finally {
    spinner.remove();
    btn.classList.remove("is-busy");
    btn.removeAttribute("aria-busy");
    btn.disabled = wasDisabled;
  }
}
