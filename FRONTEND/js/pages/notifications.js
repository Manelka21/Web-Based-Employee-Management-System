import { h, icon } from "../core/dom.js";
import { bus, EVENTS } from "../core/events.js";
import { NotificationsApi } from "../services/api.js";
import { asyncSection, emptyState, skeleton } from "../components/feedback.js";
import { button, withBusy } from "../components/button.js";
import { card, page, pageHeader } from "../components/page.js";
import { toast, toastError } from "../components/snackbar.js";
import { notificationItem } from "../widgets/notifications.js";

export default function NotificationsPage() {
  let filter = "all";
  let listSection;

  const markAll = button({
    label: "Mark all as read",
    icon: "done_all",
    variant: "tonal",
    onClick: async (event) => {
      try {
        await withBusy(event.currentTarget, () => NotificationsApi.markAllRead());
        bus.emit(EVENTS.NOTIFICATIONS);
        toast("All notifications marked as read");
        listSection.reload();
      } catch (err) {
        toastError(err);
      }
    },
  });

  const chips = h(
    "div",
    { class: "chip-set", style: { padding: "16px 24px 8px" } },
    [
      ["all", "All"],
      ["unread", "Unread"],
    ].map(([id, label]) =>
      h(
        "button",
        {
          type: "button",
          class: ["chip", "state", filter === id && "is-selected"],
          "aria-pressed": String(filter === id),
          onClick: () => {
            filter = id;
            chips.querySelectorAll(".chip").forEach((chip, i) => {
              const on = (i === 0 ? "all" : "unread") === id;
              chip.classList.toggle("is-selected", on);
              chip.setAttribute("aria-pressed", String(on));
            });
            listSection.reload();
          },
        },
        label
      )
    )
  );

  listSection = asyncSection(
    () => (filter === "unread" ? NotificationsApi.unread() : NotificationsApi.list()),
    (items) =>
      items.length
        ? h("div", { class: "list", style: { paddingBottom: "8px" } }, items.map((n) => notificationItem(n, { onOpen: () => bus.emit(EVENTS.NOTIFICATIONS) })))
        : emptyState({
            icon: filter === "unread" ? "mark_email_read" : "notifications_off",
            title: filter === "unread" ? "No unread notifications" : "No notifications yet",
            text: "Leave decisions, payslips, attendance corrections and training updates will appear here.",
          }),
    { placeholder: () => h("div", { style: { padding: "8px 24px 24px" } }, skeleton({ rows: 5 })) }
  );

  return page(
    pageHeader({ title: "Notifications", subtitle: "Updates sent to your account", actions: markAll }),
    card({ flush: true }, chips, listSection)
  );
}
