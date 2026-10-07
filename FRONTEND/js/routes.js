// Route table — one place that decides URLs, page modules (lazy-loaded), who may
// open each page, and how it appears in the navigation drawer.

import { can, hasRole, isLinked, ROLES } from "./core/permissions.js";
import { isOn } from "./core/features.js";

const linkedOr = (capability) => (user) => isLinked(user) || can(user, capability);
const notAdmin = (user) => !hasRole(user, ROLES.ADMIN);

// A route or shortcut without a `feature` is core and always on (see core/features.js)
export const routeOn = (route) => isOn(route?.feature);

export const NAV_SECTIONS = [
  { id: "overview", label: null },
  { id: "workforce", label: "Workforce" },
  { id: "time", label: "Time & pay" },
  { id: "insights", label: "Insights" },
  { id: "admin", label: "Administration" },
];

export const routes = [
  // ---- Public ----
  { path: "/login", public: true, guestOnly: true, title: "Sign in", load: () => import("./pages/auth/login.js") },
  { path: "/register", public: true, guestOnly: true, title: "Create account", load: () => import("./pages/auth/register.js") },
  // Signed-in but full-screen (no navigation): forced password change after an admin reset
  { path: "/change-password", bare: true, title: "Choose a new password", load: () => import("./pages/auth/changePassword.js") },

  // ---- Overview ----
  { path: "/", title: "Home", load: () => import("./pages/home.js"), nav: { section: "overview", label: "Home", icon: "home" } },
  {
    path: "/notifications",
    title: "Notifications",
    load: () => import("./pages/notifications.js"),
    nav: { section: "overview", label: "Notifications", icon: "notifications", counter: "unread" },
  },
  { path: "/profile", title: "My profile", load: () => import("./pages/profile.js") },

  // ---- Workforce ----
  {
    path: "/employees",
    feature: "employees",
    title: "Directory",
    allow: (u) => can(u, "listEmployees"),
    load: () => import("./pages/employees/list.js"),
    nav: { section: "workforce", label: "Directory", icon: "group", keywords: "employees people staff" },
  },
  { path: "/employees/:id", feature: "employees", title: "Employee", allow: notAdmin, navParent: "/employees", load: () => import("./pages/employees/detail.js") },
  {
    path: "/organization",
    title: "Organization",
    load: () => import("./pages/organization.js"),
    nav: { section: "workforce", label: "Organization", icon: "apartment", keywords: "departments positions" },
  },
  {
    path: "/recruitment",
    title: "Recruitment",
    allow: (u) => can(u, "recruitment"),
    load: () => import("./pages/recruitment.js"),
    nav: { section: "workforce", label: "Recruitment", icon: "work", keywords: "vacancies applications candidates hiring" },
  },
  {
    path: "/training",
    title: "Training",
    allow: notAdmin,
    load: () => import("./pages/training.js"),
    nav: { section: "workforce", label: "Training", icon: "school", keywords: "programs courses enrollments" },
  },
  {
    path: "/performance",
    feature: "performance",
    title: "Performance",
    allow: linkedOr("viewPerformance"),
    load: () => import("./pages/performance.js"),
    nav: { section: "workforce", label: "Performance", icon: "trending_up", keywords: "reviews feedback rating" },
  },

  // ---- Time & pay ----
  {
    path: "/attendance",
    feature: "attendance",
    title: "Attendance",
    allow: linkedOr("viewDepartmentAttendance"),
    load: () => import("./pages/attendance.js"),
    nav: { section: "time", label: "Attendance", icon: "schedule", keywords: "check in check out" },
  },
  {
    path: "/leave",
    feature: "leave",
    title: "Leave",
    allow: linkedOr("approveLeave"),
    load: () => import("./pages/leave.js"),
    nav: { section: "time", label: "Leave", icon: "beach_access", keywords: "holiday time off approvals" },
  },
  {
    path: "/payroll",
    feature: "payroll",
    title: "Payroll",
    allow: (u) => isLinked(u) || can(u, "viewAllPayroll") || can(u, "manageSalaries"),
    load: () => import("./pages/payroll.js"),
    nav: { section: "time", label: "Payroll", icon: "payments", keywords: "salary payslips" },
  },

  // ---- Insights ----
  {
    path: "/reports",
    feature: "reports",
    title: "Reports",
    allow: (u) => can(u, "reports"),
    load: () => import("./pages/reports.js"),
    nav: { section: "insights", label: "Reports", icon: "bar_chart", keywords: "analytics headcount summary" },
  },

  // ---- Administration ----
  {
    path: "/admin/users",
    feature: "userAdmin",
    title: "Users & access",
    allow: (u) => can(u, "manageUsers"),
    load: () => import("./pages/admin/users.js"),
    nav: { section: "admin", label: "Users & access", icon: "manage_accounts", keywords: "accounts roles passwords" },
  },
  {
    path: "/admin/activity",
    feature: "activityLog",
    title: "Activity log",
    allow: (u) => can(u, "viewActivityLogs"),
    load: () => import("./pages/admin/activity.js"),
    nav: { section: "admin", label: "Activity log", icon: "history", keywords: "audit" },
  },
  {
    path: "/admin/outbox",
    feature: "emailOutbox",
    title: "Email outbox",
    allow: (u) => can(u, "viewOutbox"),
    load: () => import("./pages/admin/outbox.js"),
    nav: { section: "admin", label: "Email outbox", icon: "outgoing_mail", keywords: "email notifications delivery" },
  },
];

// The drawer's "Create" menu — shortcuts that open a page with its create dialog
export const CREATE_ACTIONS = [
  { label: "Apply for leave", feature: "leave", icon: "beach_access", href: "#/leave?action=apply", allow: isLinked },
  { label: "Add employee", feature: "employees", icon: "person_add", href: "#/employees?action=new", allow: (u) => can(u, "manageEmployees") },
  { label: "Log attendance", feature: "attendance", icon: "more_time", href: "#/attendance?tab=team&action=log", allow: (u) => can(u, "logAttendance") },
  { label: "Record a review", feature: "performance", icon: "rate_review", href: "#/performance?tab=team&action=new", allow: (u) => can(u, "recordPerformance") },
  { label: "Generate payroll", feature: "payroll", icon: "request_quote", href: "#/payroll?tab=all&action=generate", allow: (u) => can(u, "runPayroll") },
  { label: "Post a vacancy", icon: "work", href: "#/recruitment?tab=vacancies&action=new", allow: (u) => can(u, "recruitment") },
  { label: "Add candidate", icon: "person_search", href: "#/recruitment?tab=applications&action=new", allow: (u) => can(u, "recruitment") },
  { label: "Training program", icon: "school", href: "#/training?action=new", allow: (u) => can(u, "manageTraining") },
  { label: "Department", icon: "apartment", href: "#/organization?action=new-department", allow: (u) => can(u, "manageOrganization") },
  { label: "User account", feature: "userAdmin", icon: "manage_accounts", href: "#/admin/users?action=new", allow: (u) => can(u, "manageUsers") },
];
