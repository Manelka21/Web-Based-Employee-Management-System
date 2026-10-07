// Field validators shared by every form. They mirror the backend rules in
// BACKEND/.../util/ValidationRules.java, so users see the problem before the
// request is sent. The API still validates everything (never trust the client).
//
// A validator takes the (trimmed, normalised) value and returns an error
// message, or null when the value is fine. Empty values are left to `required`.

export const PATTERNS = Object.freeze({
  // \p{M} = combining marks, needed for Sinhala and Tamil vowel signs
  name: /^\p{L}[\p{L}\p{M} .'-]*$/u,
  phone: /^(?:0\d{9}|\+94\d{9})$/,
  nic: /^(?:\d{9}[VvXx]|\d{12})$/,
  email: /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/,
});

export const v = {
  name: (label = "Name") => (value) =>
    value && !PATTERNS.name.test(value) ? `${label} can only contain letters, spaces, dots, apostrophes and hyphens` : null,

  phone: (value) =>
    value && !PATTERNS.phone.test(value) ? "Use a Sri Lankan number: 10 digits starting with 0 (0771234567) or +94 and 9 digits" : null,

  nic: (value) => (value && !PATTERNS.nic.test(value) ? "NIC must be 9 digits followed by V or X, or 12 digits" : null),

  email: (value) => (value && !PATTERNS.email.test(value) ? "Enter a valid email such as name@company.lk" : null),

  password: (value) => {
    if (!value) return null;
    if (value.length < 8 || value.length > 72) return "Use 8 to 72 characters";
    if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) return "Include at least one letter and one number";
    return null;
  },

  minLength: (min, label = "This field") => (value) => (value && value.length < min ? `${label} needs at least ${min} characters` : null),

  notFuture: (label = "Date") => (value) => (value && value > todayISO() ? `${label} can't be in the future` : null),

  notPast: (label = "Date") => (value) => (value && value < todayISO() ? `${label} can't be in the past` : null),

  range: (min, max, label = "Value") => (value) => {
    if (value == null || value === "") return null;
    const n = Number(value);
    if (Number.isNaN(n)) return `${label} must be a number`;
    if (min != null && n < min) return `${label} must be at least ${min}`;
    if (max != null && n > max) return `${label} can't be more than ${max}`;
    return null;
  },

  maxDecimals: (places, label = "Value") => (value) => {
    if (value == null || value === "") return null;
    const decimals = String(value).split(".")[1];
    return decimals && decimals.length > places ? `${label} can have at most ${places} decimal places` : null;
  },
};

// Run several validators in order; the first message wins
export const all = (...validators) => (value, form) => {
  for (const check of validators) {
    const message = check?.(value, form);
    if (message) return message;
  }
  return null;
};

// Value normalisers (applied before validating and before sending)
export const NORMALIZERS = {
  phone: (value) => value.replace(/[\s-]/g, ""),
  upper: (value) => value.toUpperCase(),
  lower: (value) => value.toLowerCase(),
};

function todayISO() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/**
 * Mirrors LeaveBalanceCalculator.workingDays on the backend: Saturdays, Sundays and
 * public holidays (a Map of "yyyy-MM-dd" -> name) don't count.
 * Returns { working, weekendDays, holidays: [names], byYear: { 2026: n, ... } }.
 */
export function workingDays(start, end, holidays = new Map()) {
  const result = { working: 0, weekendDays: 0, holidays: [], byYear: {} };
  if (!start || !end || end < start) return result;
  const [y, m, d] = start.split("-").map(Number);
  for (let t = Date.UTC(y, m - 1, d); ; t += 86400000) {
    const iso = new Date(t).toISOString().slice(0, 10);
    if (iso > end) break;
    const day = new Date(t).getUTCDay();
    if (day === 0 || day === 6) result.weekendDays++;
    else if (holidays.has(iso)) result.holidays.push(holidays.get(iso));
    else {
      result.working++;
      const year = iso.slice(0, 4);
      result.byYear[year] = (result.byYear[year] || 0) + 1;
    }
  }
  return result;
}

export function daysBetweenInclusive(start, end) {
  if (!start || !end) return 0;
  const [a, b] = [start, end].map((s) => {
    const [y, m, d] = s.split("-").map(Number);
    return Date.UTC(y, m - 1, d);
  });
  return Math.round((b - a) / 86400000) + 1;
}
