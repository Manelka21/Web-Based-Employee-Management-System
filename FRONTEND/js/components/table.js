// Data table with search, filter chips and column sorting — all client-side.
//
// columns: [{ key, label, render?(row), sortValue?(row), align?: "right", cls?, sortable? }]
// filters: [{ id, label, predicate(row) }] — first one is usually "All"

import { h, icon, debounce } from "../core/dom.js";
import { emptyState } from "./feedback.js";

export function dataTable({
  title,
  subtitle,
  headerActions,
  columns,
  rows = [],
  searchable = true,
  searchPlaceholder = "Search",
  searchText,
  filters,
  activeFilter,
  onRowClick,
  empty = {},
  toolbarExtra,
}) {
  const state = {
    rows,
    query: "",
    filter: activeFilter ?? filters?.[0]?.id,
    sortKey: null,
    sortDir: 1,
  };

  const textOf = searchText || ((row) => Object.values(row).filter((v) => v != null && typeof v !== "object").join(" "));

  const tbody = h("tbody");
  const thead = h("thead");
  const footer = h("div", { class: "table-footer" });
  const tableWrap = h("div", { class: "table-wrap" }, h("table", { class: "data-table" }, thead, tbody));
  const emptyHost = h("div", { hidden: true });

  // ---- Header ----
  const renderHead = () => {
    thead.replaceChildren(
      h(
        "tr",
        null,
        columns.map((col) => {
          const sortable = col.sortable !== false && col.key !== "actions" && col.label;
          const active = state.sortKey === col.key;
          return h(
            "th",
            {
              class: [col.align === "right" && "text-right", col.cls],
              scope: "col",
              "aria-sort": active ? (state.sortDir === 1 ? "ascending" : "descending") : null,
              style: sortable ? { cursor: "pointer", userSelect: "none" } : null,
              onClick: sortable
                ? () => {
                    state.sortDir = active ? -state.sortDir : 1;
                    state.sortKey = col.key;
                    render();
                  }
                : null,
            },
            h(
              "span",
              { class: "row", style: { "--gap": "4px", display: "inline-flex", justifyContent: col.align === "right" ? "flex-end" : "flex-start" } },
              col.label ?? "",
              active && icon(state.sortDir === 1 ? "arrow_upward" : "arrow_downward", { size: "xs" })
            )
          );
        })
      )
    );
  };

  // ---- Body ----
  const visibleRows = () => {
    let list = state.rows;
    const activeDef = filters?.find((f) => f.id === state.filter);
    if (activeDef?.predicate) list = list.filter(activeDef.predicate);
    if (state.query) {
      const q = state.query.toLowerCase();
      list = list.filter((row) => textOf(row).toLowerCase().includes(q));
    }
    if (state.sortKey) {
      const col = columns.find((c) => c.key === state.sortKey);
      const valueOf = col.sortValue || ((row) => row[col.key]);
      list = [...list].sort((a, b) => {
        const va = valueOf(a);
        const vb = valueOf(b);
        if (va == null) return 1;
        if (vb == null) return -1;
        return (typeof va === "number" && typeof vb === "number" ? va - vb : String(va).localeCompare(String(vb), undefined, { numeric: true })) * state.sortDir;
      });
    }
    return list;
  };

  const render = () => {
    renderHead();
    const list = visibleRows();
    tbody.replaceChildren(
      ...list.map((row) =>
        h(
          "tr",
          {
            class: onRowClick && "is-clickable",
            tabindex: onRowClick ? "0" : null,
            onClick: onRowClick
              ? (event) => {
                  if (event.target.closest("button, a, select, input")) return;
                  onRowClick(row);
                }
              : null,
            onKeydown: onRowClick
              ? (event) => {
                  if (event.key === "Enter" && event.target === event.currentTarget) onRowClick(row);
                }
              : null,
          },
          columns.map((col) =>
            h(
              "td",
              { class: [col.align === "right" && "text-right num", col.key === "actions" && "cell-actions", col.cls] },
              col.key === "actions" ? h("div", null, col.render(row)) : col.render ? col.render(row) : row[col.key] ?? "—"
            )
          )
        )
      )
    );

    const isEmpty = list.length === 0;
    tableWrap.hidden = isEmpty;
    emptyHost.hidden = !isEmpty;
    if (isEmpty) {
      const filtered = state.query || (filters && state.filter !== filters[0]?.id);
      emptyHost.replaceChildren(
        filtered
          ? emptyState({ icon: "search_off", title: "No matches", text: "Try a different search or filter." })
          : emptyState({ icon: empty.icon || "inbox", title: empty.title || "Nothing here yet", text: empty.text, action: empty.action })
      );
    }
    footer.textContent = state.rows.length
      ? list.length === state.rows.length
        ? `${state.rows.length} ${state.rows.length === 1 ? "item" : "items"}`
        : `${list.length} of ${state.rows.length}`
      : "";
    footer.hidden = !state.rows.length;
  };

  // ---- Toolbar ----
  const searchInput = h("input", {
    type: "search",
    placeholder: searchPlaceholder,
    "aria-label": searchPlaceholder,
    onInput: debounce((event) => {
      state.query = event.target.value.trim();
      render();
    }, 120),
  });

  const chips = filters?.length
    ? h(
        "div",
        { class: "chip-set", role: "group", "aria-label": "Filters" },
        filters.map((f) => {
          const chip = h(
            "button",
            {
              type: "button",
              class: ["chip", "state", state.filter === f.id && "is-selected"],
              "aria-pressed": String(state.filter === f.id),
              onClick: () => {
                state.filter = f.id;
                chips.querySelectorAll(".chip").forEach((c) => {
                  const on = c === chip;
                  c.classList.toggle("is-selected", on);
                  c.setAttribute("aria-pressed", String(on));
                  c.querySelector(".icon")?.remove();
                  if (on) c.prepend(icon("check"));
                });
                render();
              },
            },
            state.filter === f.id && icon("check"),
            f.label
          );
          return chip;
        })
      )
    : null;

  const hasToolbar = searchable || chips || toolbarExtra;
  const header =
    title || headerActions
      ? h(
          "div",
          { class: "card__header" },
          h("div", { style: { flex: "1", minWidth: "0" } }, title && h("h2", { class: "card__title" }, title), subtitle && h("p", { class: "card__subtitle" }, subtitle)),
          headerActions
        )
      : null;

  const root = h(
    "section",
    { class: "card card--flush" },
    header,
    hasToolbar &&
      h(
        "div",
        { class: "table-toolbar" },
        searchable && h("label", { class: "search search--compact" }, icon("search"), searchInput),
        chips,
        h("div", { class: "spacer" }),
        toolbarExtra
      ),
    tableWrap,
    emptyHost,
    footer
  );

  root.setRows = (next) => {
    state.rows = next;
    render();
  };

  render();
  return root;
}
