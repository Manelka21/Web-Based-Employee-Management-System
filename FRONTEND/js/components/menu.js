// Anchored popover menu. items: [{ label, icon, onClick, href, divider, heading }]
// or pass `content` for a custom body (e.g. the notifications panel).

import { h, icon } from "../core/dom.js";

let openMenuEl = null;

export function closeMenus() {
  openMenuEl?.close();
}

export function openMenu(anchor, { items = [], content, width, align = "end" } = {}) {
  closeMenus();

  const menu = h(
    "div",
    { class: "menu", role: "menu", style: width ? { width: `${width}px` } : null },
    content ??
      // Falsy entries (from `condition && {...}`) are skipped
      items.filter(Boolean).map((item) => {
        if (item.divider) return h("div", { class: "menu__divider", role: "separator" });
        if (item.heading) return h("div", { class: "menu__label" }, item.heading);
        const tag = item.href ? "a" : "button";
        return h(
          tag,
          {
            class: "menu__item state",
            role: "menuitem",
            href: item.href,
            type: item.href ? null : "button",
            onClick: () => {
              close();
              item.onClick?.();
            },
          },
          item.icon && icon(item.icon),
          h("span", null, item.label)
        );
      })
  );

  document.body.append(menu);

  const place = () => {
    const rect = anchor.getBoundingClientRect();
    const menuRect = menu.getBoundingClientRect();
    let left = align === "end" ? rect.right - menuRect.width : rect.left;
    left = Math.max(8, Math.min(left, window.innerWidth - menuRect.width - 8));
    let top = rect.bottom + 6;
    if (top + menuRect.height > window.innerHeight - 8) top = Math.max(8, rect.top - menuRect.height - 6);
    menu.style.left = `${left}px`;
    menu.style.top = `${top}px`;
  };
  place();

  const onDocDown = (event) => {
    if (!menu.contains(event.target) && !anchor.contains(event.target)) close();
  };
  const onKey = (event) => {
    if (event.key === "Escape") {
      close();
      anchor.focus();
    }
  };

  function close() {
    menu.remove();
    document.removeEventListener("pointerdown", onDocDown, true);
    document.removeEventListener("keydown", onKey);
    window.removeEventListener("resize", close);
    anchor.setAttribute("aria-expanded", "false");
    if (openMenuEl === handle) openMenuEl = null;
  }

  setTimeout(() => {
    document.addEventListener("pointerdown", onDocDown, true);
    document.addEventListener("keydown", onKey);
    window.addEventListener("resize", close);
  });

  anchor.setAttribute("aria-expanded", "true");
  const handle = { close, el: menu };
  openMenuEl = handle;
  menu.querySelector(".menu__item")?.focus({ preventScroll: true });
  return handle;
}
