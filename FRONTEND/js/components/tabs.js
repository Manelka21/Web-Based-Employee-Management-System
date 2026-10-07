// Tabbed view whose active tab is kept in the URL (?tab=...), so reloads and
// shared links land on the same tab.

import { h, icon } from "../core/dom.js";
import { setQuery } from "../core/router.js";

export function tabbedView({ items, active, render, queryKey = "tab" }) {
  const visible = items.filter((item) => item.visible !== false);
  let current = visible.some((item) => item.id === active) ? active : visible[0]?.id;

  const panel = h("div", { class: "tab-panel", role: "tabpanel", style: { paddingTop: "24px" } });
  const buttons = new Map();

  const select = (id, { focus = false } = {}) => {
    current = id;
    buttons.forEach((btn, key) => {
      const on = key === id;
      btn.classList.toggle("is-active", on);
      btn.setAttribute("aria-selected", String(on));
      btn.tabIndex = on ? 0 : -1;
      if (on && focus) btn.focus();
    });
    setQuery({ [queryKey]: id });
    const item = visible.find((i) => i.id === id);
    panel.replaceChildren(render(id, item));
  };

  const bar = h(
    "div",
    {
      class: "tabs",
      role: "tablist",
      onKeydown: (event) => {
        if (!["ArrowRight", "ArrowLeft"].includes(event.key)) return;
        const ids = visible.map((i) => i.id);
        const idx = ids.indexOf(current);
        const next = ids[(idx + (event.key === "ArrowRight" ? 1 : ids.length - 1)) % ids.length];
        select(next, { focus: true });
      },
    },
    visible.map((item) => {
      const btn = h(
        "button",
        { type: "button", role: "tab", class: "tab state", onClick: () => select(item.id) },
        item.icon && icon(item.icon),
        item.label,
        item.count ? h("span", { class: "tab__count" }, item.count) : null
      );
      buttons.set(item.id, btn);
      return btn;
    })
  );

  const root = h("div", null, bar, panel);
  root.select = select;
  if (current) select(current);
  return root;
}
