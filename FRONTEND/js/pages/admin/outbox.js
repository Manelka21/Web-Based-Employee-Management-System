// Email outbox (IT admin): every notification that was also queued as an email,
// whether it went out, and a retry for failures. Emails are delivered once a minute
// by the backend's NotificationDispatcher.

import { h } from "../../core/dom.js";
import { fmtDateTime, humanize } from "../../core/format.js";
import { NotificationsApi, UsersApi } from "../../services/api.js";
import { asyncSection } from "../../components/feedback.js";
import { button } from "../../components/button.js";
import { dataTable } from "../../components/table.js";
import { page, pageHeader, personCell, banner } from "../../components/page.js";
import { toast, toastError } from "../../components/snackbar.js";

const STATUS_TONE = { PENDING: "warning", SENT: "success", FAILED: "error" };

export default function OutboxPage() {
  const content = asyncSection(
    async () => {
      const [messages, users] = await Promise.all([NotificationsApi.outbox(300), UsersApi.list()]);
      return { messages, users: new Map(users.map((u) => [u.userId, u])) };
    },
    ({ messages, users }, reload) => {
      const counts = (status) => messages.filter((m) => m.deliveryStatus === status).length;
      return dataTable({
        rows: messages,
        searchPlaceholder: "Search recipients and messages",
        searchText: (m) => `${users.get(m.userId)?.fullName ?? ""} ${users.get(m.userId)?.email ?? ""} ${m.type} ${m.message}`,
        filters: [
          { id: "all", label: "All" },
          { id: "PENDING", label: `Queued (${counts("PENDING")})`, predicate: (m) => m.deliveryStatus === "PENDING" },
          { id: "SENT", label: `Sent (${counts("SENT")})`, predicate: (m) => m.deliveryStatus === "SENT" },
          { id: "FAILED", label: `Failed (${counts("FAILED")})`, predicate: (m) => m.deliveryStatus === "FAILED" },
        ],
        toolbarExtra: button({ label: "Refresh", icon: "refresh", variant: "text", size: "sm", onClick: reload }),
        columns: [
          { key: "sentAt", label: "Queued", render: (m) => h("span", { class: "nowrap" }, fmtDateTime(m.sentAt)) },
          {
            key: "userId",
            label: "To",
            render: (m) => {
              const u = users.get(m.userId);
              return u ? personCell(u.fullName, u.email) : `User #${m.userId}`;
            },
            sortValue: (m) => users.get(m.userId)?.fullName,
          },
          { key: "type", label: "Subject", render: (m) => humanize(m.type) },
          { key: "message", label: "Message", render: (m) => h("span", { class: "body-small muted truncate", title: m.message, style: { display: "inline-block", maxWidth: "320px", verticalAlign: "middle" } }, m.message) },
          { key: "deliveryStatus", label: "Status", render: (m) => h("span", { class: `status status--${STATUS_TONE[m.deliveryStatus] || "neutral"}` }, humanize(m.deliveryStatus)) },
          { key: "deliveredAt", label: "Delivered", render: (m) => fmtDateTime(m.deliveredAt) },
          {
            key: "actions",
            render: (m) =>
              m.deliveryStatus === "FAILED" &&
              button({
                label: "Retry",
                variant: "tonal",
                size: "sm",
                onClick: async () => {
                  try {
                    await NotificationsApi.retry(m.notificationId);
                    toast("Queued again. It goes out within a minute.");
                    reload();
                  } catch (err) {
                    toastError(err);
                  }
                },
              }),
          },
        ],
        empty: { icon: "outgoing_mail", title: "No emails yet", text: "Leave decisions, payslips, lockouts and password resets are emailed as well as shown in the app." },
      });
    }
  );

  return page(
    pageHeader({ title: "Email outbox", subtitle: "Emails sent alongside in-app notifications" }),
    banner(
      "info",
      "info",
      "Until an SMTP server is configured on the backend, emails are written to the application log instead of being sent. They still show as Sent here."
    ),
    h("div", { style: { marginTop: "16px" } }, content)
  );
}
