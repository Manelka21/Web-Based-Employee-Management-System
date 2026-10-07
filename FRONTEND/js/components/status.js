import { h } from "../core/dom.js";
import { humanize } from "../core/format.js";

// Every enum value the backend returns, mapped to a tone
const TONES = {
  // positive / done
  ACTIVE: "success", PRESENT: "success", APPROVED: "success", HIRED: "success",
  PAID: "success", COMPLETED: "success", FILLED: "success",
  // in flight
  OPEN: "info", FINALIZED: "info", SCHEDULED: "info", ENROLLED: "info",
  SHORTLISTED: "info", INTERVIEWED: "info", IN_PROGRESS: "info",
  // needs attention
  PENDING: "warning", PROBATION: "warning", LATE: "warning", HALF_DAY: "warning", ON_LEAVE: "warning",
  // negative
  REJECTED: "error", ABSENT: "error", VOIDED: "error",
  // neutral
  APPLIED: "neutral", DRAFT: "neutral", CLOSED: "neutral", INACTIVE: "neutral",
  CANCELLED: "neutral", WITHDRAWN: "neutral", DROPPED: "neutral",
};

export const statusBadge = (value, label) =>
  h("span", { class: `status status--${TONES[value] || "neutral"}` }, label ?? humanize(value));

export const toneOf = (value) => TONES[value] || "neutral";
