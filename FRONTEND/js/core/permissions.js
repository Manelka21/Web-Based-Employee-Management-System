// Role model. Mirrors the backend's SecurityConfig + @PreAuthorize rules so the UI
// only offers actions the API will accept. The API remains the source of truth —
// record-level rules (own team, own record) are still enforced server-side.

export const ROLES = Object.freeze({
  EMPLOYEE: "EMPLOYEE",
  HR: "HR_MANAGER",
  SUPERVISOR: "DEPT_SUPERVISOR",
  PAYROLL: "PAYROLL_EXECUTIVE",
  DIRECTOR: "COMPANY_DIRECTOR",
  ADMIN: "IT_ADMIN",
});

export const ROLE_LABELS = Object.freeze({
  EMPLOYEE: "Employee",
  HR_MANAGER: "HR Manager",
  DEPT_SUPERVISOR: "Department Supervisor",
  PAYROLL_EXECUTIVE: "Payroll Executive",
  COMPANY_DIRECTOR: "Company Director",
  IT_ADMIN: "IT Administrator",
});

const { EMPLOYEE, HR, SUPERVISOR, PAYROLL, DIRECTOR, ADMIN } = ROLES;

const CAPABILITIES = {
  // Dashboard
  viewCompanyDashboard: [DIRECTOR, HR],

  // Employees
  listEmployees: [HR, SUPERVISOR, DIRECTOR, PAYROLL],
  manageEmployees: [HR],

  // Organisation
  manageOrganization: [HR],

  // Recruitment
  recruitment: [HR],

  // Training
  manageTraining: [HR],
  enrolOthers: [HR, SUPERVISOR],
  manageEnrollments: [HR, SUPERVISOR],

  // Attendance
  logAttendance: [SUPERVISOR, HR],
  viewDepartmentAttendance: [SUPERVISOR, HR, PAYROLL, DIRECTOR],
  deleteAttendance: [HR],

  // Leave
  approveLeave: [SUPERVISOR, HR],
  viewEmployeeLeave: [SUPERVISOR, HR, PAYROLL, DIRECTOR],

  // Payroll
  runPayroll: [PAYROLL],
  viewAllPayroll: [PAYROLL, DIRECTOR],
  manageSalaries: [PAYROLL, HR],

  // Performance
  recordPerformance: [SUPERVISOR],
  viewPerformance: [SUPERVISOR, HR, DIRECTOR],
  deletePerformance: [SUPERVISOR, HR],

  // Reports
  reports: [DIRECTOR, HR, PAYROLL],
  reportHeadcount: [DIRECTOR, HR],
  reportAttendance: [DIRECTOR, HR, PAYROLL],
  reportLeave: [DIRECTOR, HR, PAYROLL],
  reportPayroll: [DIRECTOR, PAYROLL],
  reportTraining: [DIRECTOR, HR],
  reportPerformance: [DIRECTOR, HR],
  reportRecruitment: [DIRECTOR, HR],

  // Administration
  manageUsers: [ADMIN],
  viewActivityLogs: [ADMIN],
  viewOutbox: [ADMIN],
};

export const ALL_ROLES = [EMPLOYEE, HR, SUPERVISOR, PAYROLL, DIRECTOR, ADMIN];

export const hasRole = (user, ...roles) => !!user && roles.includes(user.role);

export const can = (user, capability) => !!user && (CAPABILITIES[capability] || []).includes(user.role);

// Self-service features (leave, check-in, payslips, …) need a linked employee record
export const isLinked = (user) => user?.employeeId != null;
