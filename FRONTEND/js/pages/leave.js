import { h, icon } from "../core/dom.js";
import { fmtDate, toISODate, todayISO } from "../core/format.js";
import { can, isLinked } from "../core/permissions.js";
import { takeAction } from "../core/router.js";
import { LeaveApi } from "../services/api.js";
import { employeeLabel, lookups } from "../services/lookups.js";
import { asyncSection, skeleton } from "../components/feedback.js";
import { button, iconButton } from "../components/button.js";
import { dataTable } from "../components/table.js";
import { tabbedView } from "../components/tabs.js";
import { card, page, pageHeader } from "../components/page.js";
import { byDateDesc, leaveColumns } from "../widgets/columns.js";
import { applyLeaveDialog, holidayMap } from "../widgets/dialogs.js";
import { cancelLeave, decideLeave, leaveBalanceList } from "../widgets/selfService.js";

export default function LeavePage(ctx) {
  const { user } = ctx;
  const action = takeAction(ctx);
  const linked = isLinked(user);
  const canApprove = can(user, "approveLeave");

  const apply = async () => {
    if (await applyLeaveDialog()) ctx.navigate("/leave", { tab: "mine" });
  };

  const cancellable = (r) => r.leaveStatus === "PENDING" || (r.leaveStatus === "APPROVED" && r.startDate > todayISO());

  const myView = () => {
    const balance = asyncSection(() => LeaveApi.balance(), leaveBalanceList, { placeholder: () => skeleton({ rows: 4 }) });
    const requests = asyncSection(
      async () => (await LeaveApi.mine()).sort(byDateDesc("startDate")),
      (rows, reload) =>
        dataTable({
          title: "My requests",
          rows,
          searchable: false,
          filters: [
            { id: "all", label: "All" },
            { id: "PENDING", label: "Pending", predicate: (r) => r.leaveStatus === "PENDING" },
            { id: "APPROVED", label: "Approved", predicate: (r) => r.leaveStatus === "APPROVED" },
            { id: "closed", label: "Rejected or cancelled", predicate: (r) => ["REJECTED", "CANCELLED"].includes(r.leaveStatus) },
          ],
          columns: leaveColumns({
            actions: (r) =>
              cancellable(r) &&
              button({
                label: "Cancel",
                variant: "text",
                size: "sm",
                onClick: async () => {
                  if (await cancelLeave(r)) {
                    reload();
                    balance.reload();
                  }
                },
              }),
          }),
          empty: { icon: "beach_access", title: "No leave requests yet", action: { label: "Apply for leave", icon: "add", onClick: apply } },
        })
    );

    // Public holidays in the next 3 months: they don't use up leave, so worth planning around
    const upcoming = asyncSection(
      async () => {
        const holidays = await holidayMap();
        const today = todayISO();
        const horizon = new Date();
        horizon.setMonth(horizon.getMonth() + 3);
        const until = toISODate(horizon);
        return [...holidays].filter(([date]) => date >= today && date <= until).sort(([a], [b]) => a.localeCompare(b));
      },
      (rows) =>
        rows.length
          ? h(
              "div",
              { class: "list" },
              rows.map(([date, name]) =>
                h(
                  "div",
                  { class: "list-item", style: { padding: "8px 0" } },
                  h("span", { class: "list-item__icon" }, icon("event")),
                  h("div", { class: "list-item__body" }, h("div", { class: "list-item__title" }, name), h("div", { class: "list-item__text" }, `${fmtDate(date)} · ${new Date(`${date}T00:00`).toLocaleDateString("en-GB", { weekday: "long" })}`))
                )
              )
            )
          : h("p", { class: "body-medium muted" }, "No public holidays in the next three months."),
      { placeholder: () => skeleton({ rows: 2 }) }
    );

    return h(
      "div",
      { class: "split" },
      h(
        "div",
        { class: "stack", style: { "--gap": "16px", alignContent: "start" } },
        card({ title: "Balance", subtitle: `${new Date().getFullYear()} entitlement, in working days` }, balance),
        card({ title: "Upcoming holidays", subtitle: "Not counted as leave days" }, upcoming)
      ),
      requests
    );
  };

  const approvalsView = () =>
    asyncSection(
      async () => {
        const [pending, maps] = await Promise.all([LeaveApi.pending(), lookups.maps()]);
        return { pending: pending.sort((a, b) => String(a.startDate).localeCompare(String(b.startDate))), maps };
      },
      ({ pending, maps }, reload) =>
        dataTable({
          title: "Waiting for approval",
          subtitle: "You can't approve your own requests",
          rows: pending,
          searchPlaceholder: "Search people",
          searchText: (r) => `${employeeLabel(maps.employees, r.employeeId)} ${r.leaveType} ${r.reason ?? ""}`,
          columns: leaveColumns({
            maps,
            withEmployee: true,
            actions: (r) =>
              r.employeeId !== user.employeeId && [
                iconButton({ icon: "close", label: "Reject", size: "sm", onClick: async () => (await decideLeave(r, "reject")) && reload() }),
                iconButton({ icon: "check", label: "Approve", size: "sm", variant: "tonal", onClick: async () => (await decideLeave(r, "approve")) && reload() }),
              ],
          }),
          empty: { icon: "task_alt", title: "No requests waiting", text: "You're all caught up." },
        })
    );

  const tabs = tabbedView({
    active: ctx.query.tab,
    items: [
      { id: "mine", label: "My leave", icon: "person", visible: linked },
      { id: "approvals", label: "Approvals", icon: "fact_check", visible: canApprove },
    ],
    render: (id) => (id === "approvals" ? approvalsView() : myView()),
  });

  if (action === "apply" && linked) setTimeout(apply);

  return page(
    pageHeader({
      title: "Leave",
      subtitle: "Requests, balances and approvals",
      actions: linked ? button({ label: "Apply for leave", icon: "add", onClick: apply }) : null,
    }),
    tabs
  );
}
