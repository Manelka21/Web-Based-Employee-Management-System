import { h, icon } from "../core/dom.js";

const host = () => document.getElementById("snackbars");

export function toast(message, { tone, action, duration = 4000 } = {}) {
  const container = host();
  if (!container) return;

  const dismiss = () => {
    if (!el.isConnected) return;
    el.classList.add("is-leaving");
    el.addEventListener("animationend", () => el.remove(), { once: true });
  };

  const el = h(
    "div",
    { class: ["snackbar", tone && `snackbar--${tone}`], role: tone === "error" ? "alert" : "status" },
    h("span", { class: "snackbar__text" }, message),
    action &&
      h(
        "button",
        {
          type: "button",
          class: "btn btn--text btn--sm state",
          onClick: () => {
            action.onClick();
            dismiss();
          },
        },
        action.label
      ),
    h("button", { type: "button", class: "icon-btn icon-btn--sm state", "aria-label": "Dismiss", onClick: dismiss }, icon("close"))
  );

  // Keep at most three on screen
  while (container.children.length >= 3) container.firstElementChild.remove();
  container.append(el);
  setTimeout(dismiss, duration);
}

export const toastError = (err) => toast(err?.message || String(err), { tone: "error", duration: 7000 });
