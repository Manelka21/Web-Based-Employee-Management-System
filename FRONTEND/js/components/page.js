// Page-level building blocks: header, cards, stats, key-value lists, avatars, bars.

import { h, icon } from "../core/dom.js";
import { initials } from "../core/format.js";

export function pageHeader({ title, subtitle, eyebrow, actions } = {}) {
  return h(
    "header",
    { class: "page-header" },
    h(
      "div",
      { class: "page-header__text" },
      eyebrow &&
        h("div", { class: "page-header__eyebrow" }, h("a", { href: eyebrow.href }, eyebrow.label), icon("chevron_right", { size: "xs" })),
      h("h1", { class: "page-header__title" }, title),
      subtitle && h("p", { class: "page-header__subtitle pretty" }, subtitle)
    ),
    actions && h("div", { class: "page-header__actions" }, actions)
  );
}

export function page(...children) {
  return h("div", { class: "page" }, children);
}

export function card({ title, subtitle, actions, flush = false, variant, cls, onClick } = {}, ...children) {
  return h(
    "section",
    { class: ["card", flush && "card--flush", variant && `card--${variant}`, onClick && "is-clickable", cls], onClick },
    (title || actions) &&
      h(
        "div",
        { class: "card__header" },
        h("div", { style: { flex: "1", minWidth: "0" } }, title && h("h2", { class: "card__title" }, title), subtitle && h("p", { class: "card__subtitle" }, subtitle)),
        actions
      ),
    children
  );
}

export function stat({ label, value, icon: iconName, meta, href, accent = false }) {
  return h(
    href ? "a" : "div",
    { class: ["stat", accent && "stat--accent"], href },
    h("span", { class: "stat__label" }, iconName && icon(iconName), label),
    h("span", { class: "stat__value" }, value ?? "—"),
    meta && h("span", { class: "stat__meta" }, meta)
  );
}

export function kv(items) {
  return h(
    "dl",
    { class: "kv" },
    items
      .filter(Boolean)
      .map(([key, value]) => h("div", { class: "kv__item" }, h("dt", { class: "kv__key" }, key), h("dd", { class: "kv__value" }, value ?? "—")))
  );
}

// Avatar tint from the two brand hues, chosen by name so it's stable
const AVATAR_TINTS = [
  ["#90D5FF", "#00304C"],
  ["#57B9FF", "#00263D"],
  ["#C6E8FF", "#0B3550"],
  ["#DDF1FF", "#0A68A8"],
];

export function avatar(name, { size } = {}) {
  const hash = [...String(name || "")].reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
  const [bg, fg] = AVATAR_TINTS[hash % AVATAR_TINTS.length];
  return h("span", { class: ["avatar", size && `avatar--${size}`], style: { background: bg, color: fg }, "aria-hidden": "true" }, initials(name));
}

export function personCell(name, sub) {
  return h(
    "div",
    { class: "cell-person" },
    avatar(name, { size: "sm" }),
    h("div", { style: { minWidth: "0" } }, h("div", { class: "cell-person__name truncate" }, name), sub && h("div", { class: "cell-person__sub truncate" }, sub))
  );
}

export function ratingStars(value) {
  if (value == null) return h("span", { class: "muted" }, "Not rated");
  return h(
    "span",
    { class: "rating", role: "img", "aria-label": `${value} out of 5` },
    Array.from({ length: 5 }, (_, i) => icon("star", { fill: i < value, cls: i < value ? "" : "icon--empty" }))
  );
}

export function meter(value, max, { warn = false } = {}) {
  const pct = max > 0 ? Math.min(100, Math.max(0, (value / max) * 100)) : 0;
  return h("div", { class: ["meter", warn && "meter--warn"], role: "meter", "aria-valuenow": value, "aria-valuemax": max }, h("div", { class: "meter__fill", style: { width: `${pct}%` } }));
}

export function barList(rows, { format = (v) => v } = {}) {
  const max = Math.max(1, ...rows.map((r) => Number(r.value) || 0));
  return h(
    "div",
    { class: "bar-list" },
    rows.map((row) =>
      h(
        "div",
        { class: "bar-row" },
        h("span", { class: "bar-row__label truncate", title: row.label }, row.label),
        h("div", { class: "bar-row__track" }, h("div", { class: "bar-row__fill", style: { width: `${((Number(row.value) || 0) / max) * 100}%` } })),
        h("span", { class: "bar-row__value" }, format(row.value))
      )
    )
  );
}

export function banner(tone, iconName, ...children) {
  return h("div", { class: `banner banner--${tone}`, role: tone === "error" ? "alert" : "note" }, icon(iconName), h("div", null, children));
}
