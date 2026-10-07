import { h, icon } from "../core/dom.js";
import { button } from "./button.js";

export function emptyState({ icon: iconName = "inbox", title, text, action } = {}) {
  return h(
    "div",
    { class: "empty" },
    h("div", { class: "empty__art" }, icon(iconName)),
    title && h("p", { class: "empty__title" }, title),
    text && h("p", { class: "empty__text pretty" }, text),
    action && button({ variant: "tonal", ...action })
  );
}

export function errorState(err, { onRetry } = {}) {
  const status = err?.status;
  const presets = {
    0: { icon: "cloud_off", title: "Can't reach the server" },
    401: { icon: "lock", title: "Please sign in again" },
    403: { icon: "lock", title: "You don't have access to this" },
    404: { icon: "search_off", title: "Not found" },
  };
  const preset = presets[status] || { icon: "error", title: "Something went wrong" };
  return emptyState({
    ...preset,
    text: err?.message,
    action: onRetry && status !== 403 ? { label: "Try again", icon: "refresh", onClick: onRetry } : undefined,
  });
}

export const loadingState = () => h("div", { class: "page-loading" }, h("div", { class: "spinner spinner--lg", role: "progressbar", "aria-label": "Loading" }));

export function skeleton({ rows = 4, height = 20 } = {}) {
  return h(
    "div",
    { class: "stack", style: { "--gap": "14px", padding: "8px 0" }, "aria-hidden": "true" },
    Array.from({ length: rows }, (_, i) => h("div", { class: "skeleton", style: { height: `${height}px`, width: `${90 - i * 12}%` } }))
  );
}

/**
 * Renders a loading placeholder, then render(data, reload) once loader() resolves,
 * or an error state with retry. Returns the container; call container.reload() to refresh.
 */
export function asyncSection(loader, render, { placeholder } = {}) {
  const container = h("div", { class: "async-section" });

  const load = async () => {
    container.replaceChildren(placeholder ? placeholder() : skeleton());
    try {
      const data = await loader();
      container.replaceChildren(render(data, load));
    } catch (err) {
      console.error(err);
      container.replaceChildren(errorState(err, { onRetry: load }));
    }
  };

  container.reload = load;
  load();
  return container;
}
