// Notification rendering shared by the top-bar panel, home page and inbox page.

import { h, icon } from "../core/dom.js";
import { bus, EVENTS } from "../core/events.js";
import { fmtRelative, humanize } from "../core/format.js";
import { NotificationsApi } from "../services/api.js";
import { asyncSection, emptyState, skeleton } from "../components/feedback.js";
import { button } from "../components/button.js";
import { toastError } from "../components/snackbar.js";
import { hrefOn } from "../core/features.js";

// type -> [icon, route]
export function notificationMeta(type = "") {
  const [iconName, href] = rawNotificationMeta(type);
  return [iconName, hrefOn(href)]; // no link to a switched-off page
}

function rawNotificationMeta(type) {
  const t = type.toUpperCase();
  if (t.includes("ACCESS_REQUEST")) return ["how_to_reg", "#/admin/users?filter=requests"];
  if (t === "PASSWORD_RESET_REQUEST") return ["lock_reset", "#/admin/users?filter=resets"];
  if (t === "ACCOUNT_LOCKED") return ["lock_clock", "#/profile"];
  if (t === "PASSWORD_RESET") return ["password", "#/profile"];
  if (t === "LEAVE_EXPIRED") return ["event_busy", "#/leave"];
  if (t.includes("LEAVE")) return ["beach_access", "#/leave"];
  if (t.includes("PAYSLIP") || t.includes("SALARY") || t.includes("PAYROLL")) return ["payments", "#/payroll"];
  if (t.includes("ATTENDANCE")) return ["schedule", "#/attendance"];
  if (t.includes("TRAINING") || t.includes("ENROL")) return ["school", "#/training"];
  if (t.includes("PERFORMANCE") || t.includes("REVIEW")) return ["trending_up", "#/performance"];
  if (t.includes("ACCOUNT") || t.includes("ROLE") || t.includes("PASSWORD")) return ["manage_accounts", "#/profile"];
  return ["notifications", null];
}

export function notificationItem(n, { onOpen } = {}) {
  const [iconName, href] = notificationMeta(n.type);
  const unread = !n.readStatus;

  const open = async (event) => {
    if (unread) {
      try {
        await NotificationsApi.markRead(n.notificationId);
        n.readStatus = true;
        bus.emit(EVENTS.NOTIFICATIONS);
      } catch (err) {
        toastError(err);
      }
    }
    onOpen?.(n);
    if (!href) event.preventDefault();
  };

  return h(
    "a",
    { class: "list-item state", href: href || "#/notifications", onClick: open },
    h("span", { class: "list-item__icon", style: unread ? null : { background: "var(--md-surface-container)", color: "var(--md-on-surface-variant)" } }, icon(iconName)),
    h(
      "span",
      { class: "list-item__body" },
      h("span", { class: "list-item__title pretty", style: { display: "block", fontWeight: unread ? "500" : "400" } }, n.message),
      h("span", { class: "list-item__text", style: { display: "block" } }, `${humanize(n.type)} · ${fmtRelative(n.sentAt)}`)
    ),
    unread && h("span", { style: { width: "8px", height: "8px", borderRadius: "50%", background: "var(--md-primary)", flexShrink: "0" }, "aria-label": "Unread" })
  );
}

export function notificationsPanel({ onNavigate, onChange }) {
  const list = asyncSection(
    () => NotificationsApi.list(),
    (items) =>
      items.length
        ? h("div", { class: "list" }, items.slice(0, 6).map((n) => notificationItem(n, { onOpen: onNavigate })))
        : emptyState({ icon: "notifications_off", title: "You're all caught up" }),
    { placeholder: () => h("div", { style: { padding: "8px 16px" } }, skeleton({ rows: 3 })) }
  );

  return h(
    "div",
    null,
    h(
      "div",
      { class: "row", style: { padding: "4px 8px 8px 16px" } },
      h("span", { class: "title-medium", style: { flex: "1" } }, "Notifications"),
      button({
        label: "Mark all read",
        variant: "text",
        size: "sm",
        onClick: async () => {
          try {
            await NotificationsApi.markAllRead();
            bus.emit(EVENTS.NOTIFICATIONS);
            onChange?.();
            list.reload();
          } catch (err) {
            toastError(err);
          }
        },
      })
    ),
    h("div", { class: "menu__divider", style: { margin: "0" } }),
    list,
    h("div", { class: "menu__divider", style: { margin: "0 0 8px" } }),
    h("div", { style: { padding: "0 8px" } }, button({ label: "See all notifications", variant: "text", href: "#/notifications", cls: "btn--block" }))
  );
}
