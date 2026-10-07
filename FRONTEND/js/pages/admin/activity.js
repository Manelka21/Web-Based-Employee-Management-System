// Audit trail (IT admin): who did what, and when.

import { h } from "../../core/dom.js";
import { fmtDateTime, humanize } from "../../core/format.js";
import { ActivityApi, UsersApi } from "../../services/api.js";
import { asyncSection } from "../../components/feedback.js";
import { compactSelect } from "../../components/fields.js";
import { dataTable } from "../../components/table.js";
import { page, pageHeader, personCell } from "../../components/page.js";

export default function ActivityPage() {
  const content = asyncSection(
    () => UsersApi.list(),
    (users) => {
      const byId = new Map(users.map((u) => [u.userId, u]));

      const userControl = compactSelect({
        label: "User",
        emptyLabel: "Everyone",
        options: [...users].sort((a, b) => a.fullName.localeCompare(b.fullName)).map((u) => ({ value: u.userId, label: u.fullName })),
        onChange: () => logs.reload(),
      });
      const limitControl = compactSelect({
        label: "Show",
        value: 100,
        options: [50, 100, 250, 500].map((n) => ({ value: n, label: `Latest ${n}` })),
        onChange: () => logs.reload(),
      });

      const logs = asyncSection(
        () => {
          const userId = userControl.control.value;
          limitControl.control.disabled = !!userId;
          return userId ? ActivityApi.byUser(userId) : ActivityApi.recent(limitControl.control.value);
        },
        (rows) =>
          dataTable({
            rows: [...rows].sort((a, b) => String(b.timestamp).localeCompare(String(a.timestamp))),
            toolbarExtra: [userControl, limitControl],
            searchPlaceholder: "Search actions and details",
            searchText: (r) => `${r.action} ${r.details ?? ""} ${byId.get(r.userId)?.fullName ?? ""}`,
            filters: [
              { id: "all", label: "All" },
              { id: "login", label: "Sign-ins", predicate: (r) => r.action === "LOGIN" },
              { id: "changes", label: "Changes", predicate: (r) => r.action !== "LOGIN" },
            ],
            columns: [
              { key: "timestamp", label: "When", render: (r) => h("span", { class: "nowrap" }, fmtDateTime(r.timestamp)) },
              {
                key: "userId",
                label: "User",
                render: (r) => {
                  const u = byId.get(r.userId);
                  return u ? personCell(u.fullName, u.email) : `User #${r.userId}`;
                },
                sortValue: (r) => byId.get(r.userId)?.fullName,
              },
              { key: "action", label: "Action", render: (r) => h("span", { class: "status status--neutral", title: r.action }, humanize(r.action)) },
              { key: "details", label: "Details", render: (r) => h("span", { class: "body-medium muted" }, r.details || "—") },
              { key: "ipAddress", label: "IP address", render: (r) => h("span", { class: "mono muted" }, r.ipAddress || "—") },
            ],
            empty: { icon: "history", title: "No activity recorded" },
          })
      );
      return logs;
    }
  );

  return page(pageHeader({ title: "Activity log", subtitle: "An audit trail of sign-ins and changes across the system" }), content);
}
