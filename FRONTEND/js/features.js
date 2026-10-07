// LankaTech EMS feature switches - written by switch-features.bat.
// Run switch-features.bat to change them, then refresh the browser.
// STATE: ON
//
// Always on and not listed: Department management, Onboarding and recruitment,
// Training, plus Sign in, Home, Profile and Notifications.
// A missing file or key counts as true, so deleting this file turns everything on.
window.EMS_FEATURES = {
  employees: true,
  attendance: true,
  leave: true,
  payroll: true,
  performance: true,
  reports: true,
  userAdmin: true,
  activityLog: true,
  emailOutbox: true,
};
