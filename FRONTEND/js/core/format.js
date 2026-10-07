// Formatting helpers. The API sends dates as "yyyy-MM-dd", times as "HH:mm:ss" and
// date-times as "yyyy-MM-ddTHH:mm:ss" (no zone — server local time).

const dateFmt = new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", year: "numeric" });
const shortDateFmt = new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short" });
const dateTimeFmt = new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
const weekdayFmt = new Intl.DateTimeFormat("en-GB", { weekday: "long", day: "numeric", month: "long" });
const moneyFmt = new Intl.NumberFormat("en-LK", { style: "currency", currency: "LKR", minimumFractionDigits: 2 });
const numberFmt = new Intl.NumberFormat("en-GB");

export function parseDate(value) {
  if (!value) return null;
  if (value instanceof Date) return value;
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [y, m, d] = value.split("-").map(Number);
    return new Date(y, m - 1, d);
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export const fmtDate = (value) => (parseDate(value) ? dateFmt.format(parseDate(value)) : "—");
export const fmtShortDate = (value) => (parseDate(value) ? shortDateFmt.format(parseDate(value)) : "—");
export const fmtDateTime = (value) => (parseDate(value) ? dateTimeFmt.format(parseDate(value)) : "—");
export const fmtWeekday = (value = new Date()) => weekdayFmt.format(parseDate(value));
export const fmtTime = (value) => (value ? String(value).slice(0, 5) : "—");
export const fmtNumber = (value) => (value == null ? "—" : numberFmt.format(Number(value)));

export function fmtMoney(value) {
  if (value == null || value === "") return "—";
  return moneyFmt.format(Number(value));
}

export function fmtRange(start, end) {
  if (!start) return "—";
  if (!end || start === end) return fmtDate(start);
  const a = parseDate(start);
  const b = parseDate(end);
  return a.getFullYear() === b.getFullYear() ? `${fmtShortDate(start)} – ${fmtDate(end)}` : `${fmtDate(start)} – ${fmtDate(end)}`;
}

export function fmtRelative(value) {
  const date = parseDate(value);
  if (!date) return "—";
  const seconds = Math.round((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) return "Just now";
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  const days = Math.round(hours / 24);
  if (days < 7) return `${days} d ago`;
  return fmtDate(date);
}

// "ON_LEAVE" -> "On leave", "HR_MANAGER" -> "Hr manager" (roles use ROLE_LABELS instead)
export function humanize(value) {
  if (value == null || value === "") return "—";
  const text = String(value).replace(/_/g, " ").toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function initials(name = "") {
  const parts = String(name).trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return "?";
  return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase();
}

export const fullName = (person) => (person ? `${person.firstName ?? ""} ${person.lastName ?? ""}`.trim() : "");

export function toISODate(date = new Date()) {
  const d = parseDate(date);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export const todayISO = () => toISODate(new Date());

export function monthBounds(date = new Date()) {
  const d = parseDate(date);
  return {
    start: toISODate(new Date(d.getFullYear(), d.getMonth(), 1)),
    end: toISODate(new Date(d.getFullYear(), d.getMonth() + 1, 0)),
  };
}

export function daysInclusive(start, end) {
  const a = parseDate(start);
  const b = parseDate(end);
  if (!a || !b) return 0;
  return Math.round((b - a) / 86400000) + 1;
}

export function greeting(date = new Date()) {
  const hour = date.getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}
