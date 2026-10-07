// Application shell: top app bar (brand, search, notifications, account),
// navigation drawer / rail with the Create FAB, and the content panel.

import { h, icon, debounce } from "../core/dom.js";
import { bus, EVENTS } from "../core/events.js";
import { navigate } from "../core/router.js";
import { can, hasRole, ROLE_LABELS, ROLES } from "../core/permissions.js";
import { fullName } from "../core/format.js";
import { NotificationsApi } from "../services/api.js";
import { lookups } from "../services/lookups.js";
import { routes, routeOn, NAV_SECTIONS, CREATE_ACTIONS } from "../routes.js";
import { isOn } from "../core/features.js";
import { iconButton, button } from "../components/button.js";
import { avatar } from "../components/page.js";
import { openMenu, closeMenus } from "../components/menu.js";
import { notificationsPanel } from "../widgets/notifications.js";

const RAIL_PREF = "ems.nav.rail";
const MOBILE_QUERY = window.matchMedia("(max-width: 860px)");

function readRailPref() {
  try {
    return localStorage.getItem(RAIL_PREF) === "1";
  } catch {
    return false;
  }
}

function writeRailPref(on) {
  try {
    localStorage.setItem(RAIL_PREF, on ? "1" : "0");
  } catch {
    /* storage unavailable — preference just won't persist */
  }
}

export function createShell({ user, onSignOut }) {
  const cleanups = [];
  const allowedNav = routes.filter((r) => r.nav && routeOn(r) && (!r.allow || r.allow(user)));

  const app = h("div", { class: ["app", readRailPref() && "is-rail"] });

  // ---------------------------------------------------------------- Drawer
  const navLinks = new Map();
  const counters = [];

  const sections = NAV_SECTIONS.map((section) => {
    const items = allowedNav.filter((r) => r.nav.section === section.id);
    if (!items.length) return null;
    return [
      section.label && h("div", { class: "drawer__section" }, section.label),
      items.map((route) => {
        const counter = route.nav.counter ? h("span", { class: "nav-item__count", hidden: true }) : null;
        if (counter) counters.push(counter);
        const link = h(
          "a",
          { class: "nav-item state", href: `#${route.path}`, title: route.nav.label },
          icon(route.nav.icon),
          h("span", { class: "nav-item__label" }, route.nav.label),
          counter
        );
        navLinks.set(route.path, link);
        return link;
      }),
    ];
  });

  const createItems = CREATE_ACTIONS.filter((a) => routeOn(a) && a.allow(user));
  const fab = createItems.length
    ? h(
        "button",
        {
          type: "button",
          class: "fab state drawer__fab",
          "aria-haspopup": "menu",
          onClick: (event) =>
            openMenu(event.currentTarget, {
              align: "start",
              items: [{ heading: "Create" }, ...createItems.map((a) => ({ label: a.label, icon: a.icon, onClick: () => (location.hash = a.href) }))],
            }),
        },
        icon("add"),
        h("span", { class: "fab__label" }, "Create")
      )
    : null;

  const drawer = h("nav", { class: "drawer", "aria-label": "Main navigation" }, fab, sections);

  // ---------------------------------------------------------------- Top bar
  const toggleNav = () => {
    if (MOBILE_QUERY.matches) {
      setDrawerOpen(!app.classList.contains("is-drawer-open"));
    } else {
      const next = !app.classList.contains("is-rail");
      app.classList.toggle("is-rail", next);
      writeRailPref(next);
    }
  };

  let scrim = null;
  function setDrawerOpen(open) {
    app.classList.toggle("is-drawer-open", open);
    if (open && !scrim) {
      scrim = h("div", { class: "scrim", onClick: () => setDrawerOpen(false) });
      document.body.append(scrim);
    } else if (!open && scrim) {
      scrim.remove();
      scrim = null;
    }
  }

  const brand = h(
    "a",
    { class: "topbar__brand", href: "#/", "aria-label": "LankaTech EMS home" },
    h("img", { class: "topbar__logo", src: "assets/logo.svg", alt: "" }),
    h("span", { class: "topbar__name" }, "LankaTech ", h("span", null, "EMS"))
  );

  const search = createSearch(user, allowedNav);

  const badge = h("span", { class: "badge", hidden: true });
  const bellBtn = iconButton({
    icon: "notifications",
    label: "Notifications",
    onClick: (event) => {
      const handle = openMenu(event.currentTarget, {
        width: 380,
        content: notificationsPanel({
          onNavigate: () => handle.close(),
          onChange: refreshUnread,
        }),
      });
    },
  });
  bellBtn.setAttribute("aria-haspopup", "dialog");
  bellBtn.append(badge);

  const accountBtn = h(
    "button",
    {
      type: "button",
      class: "topbar__avatar state",
      "aria-label": `Account: ${user.fullName}`,
      "aria-haspopup": "menu",
      onClick: (event) =>
        openMenu(event.currentTarget, {
          width: 300,
          content: [
            h(
              "div",
              { class: "stack", style: { "--gap": "4px", alignItems: "center", padding: "16px 16px 12px", textAlign: "center" } },
              avatar(user.fullName, { size: "lg" }),
              h("div", { class: "title-medium", style: { marginTop: "8px" } }, user.fullName),
              h("div", { class: "body-medium muted truncate", style: { maxWidth: "100%" } }, user.email),
              h("span", { class: "status status--info", style: { marginTop: "6px" } }, ROLE_LABELS[user.role] || user.role)
            ),
            h("div", { class: "menu__divider" }),
            h("a", { class: "menu__item state", href: "#/profile", onClick: closeMenus }, icon("account_circle"), "My profile"),
            h("a", { class: "menu__item state", href: "#/notifications", onClick: closeMenus }, icon("notifications"), "Notifications"),
            h("div", { class: "menu__divider" }),
            h(
              "button",
              {
                type: "button",
                class: "menu__item state",
                onClick: () => {
                  closeMenus();
                  onSignOut();
                },
              },
              icon("logout"),
              "Sign out"
            ),
          ],
        }),
    },
    avatar(user.fullName, { size: "sm" })
  );

  const topbar = h(
    "header",
    { class: "topbar" },
    iconButton({ icon: "menu", label: "Main menu", onClick: toggleNav }),
    brand,
    search.el,
    h("div", { class: "topbar__actions" }, bellBtn, accountBtn)
  );

  // ---------------------------------------------------------------- Content
  const panel = h("main", { class: "main__panel", id: "content", tabindex: "-1" });
  app.append(topbar, drawer, h("div", { class: "main" }, panel));

  // ---------------------------------------------------------------- Unread counter
  async function refreshUnread() {
    try {
      const { unread } = await NotificationsApi.unreadCount();
      badge.hidden = !unread;
      badge.textContent = unread > 99 ? "99+" : String(unread);
      counters.forEach((c) => {
        c.hidden = !unread;
        c.textContent = String(unread);
      });
    } catch {
      /* the badge is non-critical */
    }
  }
  refreshUnread();
  const poll = setInterval(refreshUnread, window.EMS_CONFIG?.notificationPollMs || 60000);
  cleanups.push(() => clearInterval(poll));
  cleanups.push(bus.on(EVENTS.NOTIFICATIONS, refreshUnread));

  const onMediaChange = () => setDrawerOpen(false);
  MOBILE_QUERY.addEventListener("change", onMediaChange);
  cleanups.push(() => MOBILE_QUERY.removeEventListener("change", onMediaChange));
  cleanups.push(search.destroy);

  return {
    el: app,
    outlet: panel,

    setActive(route) {
      const activePath = route?.navParent || route?.path;
      navLinks.forEach((link, path) => {
        const on = path === activePath;
        link.classList.toggle("is-active", on);
        if (on) link.setAttribute("aria-current", "page");
        else link.removeAttribute("aria-current");
      });
      setDrawerOpen(false);
    },

    refreshUnread,

    destroy() {
      cleanups.forEach((fn) => fn());
      setDrawerOpen(false);
      closeMenus();
      app.remove();
    },
  };
}

// ------------------------------------------------------------------ Search
// Finds pages the user can open and, for roles that can list employees, people.
function createSearch(user, allowedNav) {
  // People results open employee pages, so they go when the directory is switched off
  const canSearchPeople = can(user, "listEmployees") && isOn("employees");
  const placeholder = canSearchPeople ? "Search pages and people" : "Search pages";
  const input = h("input", {
    type: "search",
    placeholder,
    "aria-label": placeholder,
    autocomplete: "off",
    role: "combobox",
    "aria-expanded": "false",
    "aria-controls": "search-results",
  });
  const results = h("div", { class: "search-results", id: "search-results", role: "listbox", hidden: true });
  const el = h("div", { class: "topbar__search" }, h("label", { class: "search" }, icon("search"), input), results);

  let people = null;
  let options = [];
  let activeIndex = -1;

  const loadPeople = async () => {
    if (!canSearchPeople || people) return;
    try {
      people = await (hasRole(user, ROLES.SUPERVISOR) ? lookups.team() : lookups.employees());
    } catch {
      people = [];
    }
  };

  const close = () => {
    results.hidden = true;
    input.setAttribute("aria-expanded", "false");
    activeIndex = -1;
  };

  const go = (option) => {
    close();
    input.value = "";
    input.blur();
    navigate(option.path);
  };

  const renderResults = () => {
    const q = input.value.trim().toLowerCase();
    if (!q) return close();

    const pageMatches = allowedNav
      .filter((r) => `${r.nav.label} ${r.nav.keywords || ""}`.toLowerCase().includes(q))
      .slice(0, 5)
      .map((r) => ({ kind: "page", label: r.nav.label, sub: null, icon: r.nav.icon, path: r.path }));

    const peopleMatches = (people || [])
      .filter((e) => `${fullName(e)} ${e.email} ${e.nic}`.toLowerCase().includes(q))
      .slice(0, 6)
      .map((e) => ({ kind: "person", label: fullName(e), sub: e.email, path: `/employees/${e.employeeId}` }));

    options = [...pageMatches, ...peopleMatches];
    activeIndex = options.length ? 0 : -1;

    const item = (opt, index) =>
      h(
        "button",
        {
          type: "button",
          class: ["list-item", "state", index === activeIndex && "is-active"],
          role: "option",
          "aria-selected": String(index === activeIndex),
          onMousedown: (event) => event.preventDefault(),
          onClick: () => go(opt),
        },
        opt.kind === "person" ? avatar(opt.label, { size: "sm" }) : h("span", { class: "list-item__icon" }, icon(opt.icon)),
        h("span", { class: "list-item__body" }, h("span", { class: "list-item__title" }, opt.label), opt.sub && h("span", { class: "list-item__text", style: { display: "block" } }, opt.sub))
      );

    results.replaceChildren(
      ...(options.length
        ? [
            pageMatches.length && h("div", { class: "menu__label" }, "Pages"),
            ...pageMatches.map((opt) => item(opt, options.indexOf(opt))),
            peopleMatches.length && h("div", { class: "menu__label" }, "People"),
            ...peopleMatches.map((opt) => item(opt, options.indexOf(opt))),
          ].filter(Boolean)
        : [h("div", { class: "list-item" }, h("span", { class: "list-item__text" }, `No results for “${input.value.trim()}”`))])
    );
    results.hidden = false;
    input.setAttribute("aria-expanded", "true");
  };

  const highlight = () => {
    results.querySelectorAll("[role=option]").forEach((node, i) => {
      node.classList.toggle("is-active", i === activeIndex);
      node.setAttribute("aria-selected", String(i === activeIndex));
      if (i === activeIndex) node.scrollIntoView({ block: "nearest" });
    });
  };

  input.addEventListener("focus", loadPeople);
  input.addEventListener("input", debounce(renderResults, 80));
  input.addEventListener("blur", () => setTimeout(close, 120));
  input.addEventListener("keydown", (event) => {
    if (event.key === "ArrowDown" && options.length) {
      event.preventDefault();
      activeIndex = (activeIndex + 1) % options.length;
      highlight();
    } else if (event.key === "ArrowUp" && options.length) {
      event.preventDefault();
      activeIndex = (activeIndex - 1 + options.length) % options.length;
      highlight();
    } else if (event.key === "Enter" && options[activeIndex]) {
      event.preventDefault();
      go(options[activeIndex]);
    } else if (event.key === "Escape") {
      close();
      input.blur();
    }
  });

  // "/" focuses search, as in Google apps
  const onSlash = (event) => {
    if (event.key !== "/" || event.target.closest("input, textarea, select, [contenteditable]")) return;
    event.preventDefault();
    input.focus();
  };
  document.addEventListener("keydown", onSlash);

  return { el, destroy: () => document.removeEventListener("keydown", onSlash) };
}
