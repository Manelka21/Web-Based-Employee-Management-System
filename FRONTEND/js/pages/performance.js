import { h, icon } from "../core/dom.js";
import { fmtDate } from "../core/format.js";
import { can, hasRole, isLinked, ROLES } from "../core/permissions.js";
import { takeAction } from "../core/router.js";
import { PerformanceApi } from "../services/api.js";
import { employeeLabel, lookups } from "../services/lookups.js";
import { asyncSection, emptyState } from "../components/feedback.js";
import { button, iconButton } from "../components/button.js";
import { compactSelect } from "../components/fields.js";
import { dataTable } from "../components/table.js";
import { tabbedView } from "../components/tabs.js";
import { card, page, pageHeader, ratingStars, stat } from "../components/page.js";
import { byDateDesc, reviewColumns } from "../widgets/columns.js";
import { deleteReview, reviewDialog } from "../widgets/dialogs.js";

export default function PerformancePage(ctx) {
  const { user } = ctx;
  const action = takeAction(ctx);
  const linked = isLinked(user);
  const isSupervisor = hasRole(user, ROLES.SUPERVISOR);
  const canRecord = can(user, "recordPerformance");

  const record = async () => {
    if (await reviewDialog({ selfEmployeeId: user.employeeId })) ctx.navigate("/performance", { tab: "team" });
  };

  // ---- My feedback ----
  const myView = () =>
    asyncSection(
      async () => (await PerformanceApi.mine()).sort(byDateDesc("reviewDate")),
      (reviews) => {
        if (!reviews.length) return emptyState({ icon: "rate_review", title: "No feedback yet", text: "Reviews from your supervisor will appear here." });
        const rated = reviews.filter((r) => r.rating != null);
        const average = rated.length ? rated.reduce((s, r) => s + r.rating, 0) / rated.length : null;

        return h(
          "div",
          { class: "stack" },
          h(
            "div",
            { class: "grid grid--3" },
            stat({ label: "Average rating", icon: "star", value: average != null ? average.toFixed(1) : "—", meta: rated.length ? `From ${rated.length} rated review${rated.length === 1 ? "" : "s"}` : "No ratings yet", accent: true }),
            stat({ label: "Reviews", icon: "rate_review", value: String(reviews.length) }),
            stat({ label: "Latest review", icon: "event", value: fmtDate(reviews[0].reviewDate) })
          ),
          reviews.map((r) =>
            card(
              {},
              h(
                "div",
                { class: "stack", style: { "--gap": "10px" } },
                h("div", { class: "row row--between row--wrap" }, ratingStars(r.rating), h("span", { class: "body-small muted" }, fmtDate(r.reviewDate))),
                h("p", { class: "body-large pretty", style: { whiteSpace: "pre-line" } }, r.feedback),
                h("span", { class: "row body-small muted", style: { "--gap": "6px" } }, icon("supervisor_account", { size: "xs" }), "From your supervisor")
              )
            )
          )
        );
      }
    );

  // ---- Team ----
  const teamView = () =>
    asyncSection(
      async () => ({
        departments: isSupervisor ? [] : await lookups.departments(),
        me: isSupervisor ? await lookups.myEmployee() : null,
      }),
      ({ departments, me }) => {
        if (isSupervisor && !me?.departmentId) throw Object.assign(new Error("Your employee record isn't assigned to a department."), { status: 403 });

        const deptControl = isSupervisor
          ? null
          : compactSelect({ label: "Department", value: departments[0]?.departmentId, options: departments.map((d) => ({ value: d.departmentId, label: d.name })), onChange: () => table.reload() });

        const table = asyncSection(
          async () => {
            const deptId = isSupervisor ? me.departmentId : Number(deptControl.control.value);
            const [rows, maps] = await Promise.all([deptId ? PerformanceApi.byTeam(deptId) : [], lookups.maps()]);
            return { rows: rows.sort(byDateDesc("reviewDate")), maps };
          },
          ({ rows, maps }, reload) =>
            dataTable({
              rows,
              toolbarExtra: deptControl,
              searchPlaceholder: "Search people or feedback",
              searchText: (r) => `${employeeLabel(maps.employees, r.employeeId)} ${r.feedback}`,
              filters: [
                { id: "all", label: "All" },
                { id: "high", label: "4 stars and up", predicate: (r) => r.rating >= 4 },
                { id: "low", label: "2 stars or below", predicate: (r) => r.rating != null && r.rating <= 2 },
              ],
              columns: reviewColumns({
                maps,
                withEmployee: true,
                actions: can(user, "deletePerformance")
                  ? (r) => [
                      isSupervisor && r.supervisorId === user.userId && iconButton({ icon: "edit", label: "Edit", size: "sm", onClick: async () => (await reviewDialog({ existing: r })) && reload() }),
                      (hasRole(user, ROLES.HR) || r.supervisorId === user.userId) && iconButton({ icon: "delete", label: "Delete", size: "sm", onClick: async () => (await deleteReview(r)) && reload() }),
                    ]
                  : null,
              }),
              empty: { icon: "rate_review", title: "No reviews recorded", action: canRecord ? { label: "Record a review", icon: "rate_review", onClick: record } : undefined },
            })
        );
        return table;
      }
    );

  const tabs = tabbedView({
    active: ctx.query.tab,
    items: [
      { id: "mine", label: "My feedback", icon: "person", visible: linked },
      { id: "team", label: isSupervisor ? "My team" : "By department", icon: "groups", visible: can(user, "viewPerformance") },
    ],
    render: (id) => (id === "team" ? teamView() : myView()),
  });

  if (action === "new" && canRecord) setTimeout(record);

  return page(
    pageHeader({
      title: "Performance",
      subtitle: "Reviews and feedback",
      actions: canRecord ? button({ label: "Record a review", icon: "rate_review", onClick: record }) : null,
    }),
    tabs
  );
}
