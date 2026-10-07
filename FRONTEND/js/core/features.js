// Feature switches. The on/off state lives in js/features.js (a classic script that
// sets window.EMS_FEATURES), which FRONTEND/switch-features.bat writes. A missing
// file or missing key means "on", so the app works without the switch file.
//
// Department management, onboarding & recruitment and training are core and are
// never switched off: they have no key here and ignore the switch file.

export const SWITCHABLE = Object.freeze({
  employees: "Employee directory",
  attendance: "Attendance",
  leave: "Leave & holidays",
  payroll: "Payroll & salaries",
  performance: "Performance",
  reports: "Reports",
  userAdmin: "Users & access",
  activityLog: "Activity log",
  emailOutbox: "Email outbox",
});

export function isOn(feature) {
  if (!feature || !(feature in SWITCHABLE)) return true;
  return window.EMS_FEATURES?.[feature] !== false;
}

// Page path -> the switch that owns it
const PATH_FEATURES = [
  ["/employees", "employees"],
  ["/attendance", "attendance"],
  ["/leave", "leave"],
  ["/payroll", "payroll"],
  ["/performance", "performance"],
  ["/reports", "reports"],
  ["/admin/users", "userAdmin"],
  ["/admin/activity", "activityLog"],
  ["/admin/outbox", "emailOutbox"],
];

// Returns the link unchanged, or null when it points at a switched-off page
export function hrefOn(href) {
  if (!href) return href;
  const path = href.replace(/^#/, "").split("?")[0];
  const owner = PATH_FEATURES.find(([prefix]) => path === prefix || path.startsWith(`${prefix}/`));
  return !owner || isOn(owner[1]) ? href : null;
}
