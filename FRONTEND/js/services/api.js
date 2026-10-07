// API service layer — one object per backend controller, one method per endpoint.
// Paths and payloads match BACKEND/src/main/java/com/lankatech/ems/controller/*.

import { http } from "../core/http.js";

// /api/auth
export const AuthApi = {
  login: (email, password) => http.post("/auth/login", { email, password }),
  logout: () => http.post("/auth/logout"),
  me: () => http.get("/auth/me"),
  register: (body) => http.post("/auth/register", body), // { email, nic, password, phoneNumber, role? }
  changePassword: (currentPassword, newPassword) => http.post("/auth/change-password", { currentPassword, newPassword }),
  forgotPassword: (email) => http.post("/auth/forgot-password", { email }),
};

// /api/users — IT_ADMIN
export const UsersApi = {
  list: () => http.get("/users"),
  get: (id) => http.get(`/users/${id}`),
  byRole: (role) => http.get(`/users/role/${role}`),
  create: (body) => http.post("/users", body), // { email, password, firstName, lastName, phoneNumber, role }
  changeRole: (id, role) => http.patch(`/users/${id}/role`, { role }),
  linkEmployee: (id, employeeId) => http.patch(`/users/${id}/link-employee/${employeeId}`),
  resetPassword: (id, newPassword) => http.patch(`/users/${id}/password`, { newPassword }),
  disable: (id) => http.patch(`/users/${id}/disable`),
  enable: (id) => http.patch(`/users/${id}/enable`),
  unlock: (id) => http.patch(`/users/${id}/unlock`),
};

// /api/holidays — everyone reads, HR maintains
export const HolidaysApi = {
  list: (year) => http.get("/holidays", { year }),
  add: (body) => http.post("/holidays", body), // { holidayDate, name }
  remove: (date) => http.del(`/holidays/${date}`),
};

// /api/employees
export const EmployeesApi = {
  list: () => http.get("/employees"),
  get: (id) => http.get(`/employees/${id}`),
  me: () => http.get("/employees/me"),
  updateMe: (body) => http.put("/employees/me", body), // { phone, address }
  byDepartment: (deptId) => http.get(`/employees/department/${deptId}`),
  create: (body) => http.post("/employees", body),
  update: (id, body) => http.put(`/employees/${id}`, body),
  remove: (id) => http.del(`/employees/${id}`), // permanent, with the employee's records
};

// /api/departments
export const DepartmentsApi = {
  list: () => http.get("/departments"),
  get: (id) => http.get(`/departments/${id}`),
  create: (body) => http.post("/departments", body), // { name, description, headOfDeptId, status }
  update: (id, body) => http.put(`/departments/${id}`, body),
  remove: (id) => http.del(`/departments/${id}`), // only empty departments; positions go with it
};

// /api/positions
export const PositionsApi = {
  list: () => http.get("/positions"),
  get: (id) => http.get(`/positions/${id}`),
  byDepartment: (deptId) => http.get(`/positions/department/${deptId}`),
  create: (body) => http.post("/positions", body), // { title, departmentId, description }
  update: (id, body) => http.put(`/positions/${id}`, body),
  remove: (id) => http.del(`/positions/${id}`),
};

// /api/vacancies — HR_MANAGER
export const VacanciesApi = {
  list: (status) => http.get("/vacancies", { status }),
  get: (id) => http.get(`/vacancies/${id}`),
  create: (body) => http.post("/vacancies", body), // { title, departmentId, requirements, deadline }
  update: (id, body) => http.put(`/vacancies/${id}`, body),
  close: (id) => http.patch(`/vacancies/${id}/close`),
  setStatus: (id, status) => http.patch(`/vacancies/${id}/status`, { status }),
  remove: (id) => http.del(`/vacancies/${id}`),
};

// /api/applications — HR_MANAGER
export const ApplicationsApi = {
  list: () => http.get("/applications"),
  get: (id) => http.get(`/applications/${id}`),
  byVacancy: (vacancyId) => http.get(`/applications/vacancy/${vacancyId}`),
  create: (body) => http.post("/applications", body),
  update: (id, body) => http.put(`/applications/${id}`, body), // candidate details

  setStatus: (id, status) => http.patch(`/applications/${id}/status`, { status }), // -> { application, createdEmployee }
  withdraw: (id) => http.del(`/applications/${id}`),
};

// /api/training-programs + /api/training-enrollments
export const TrainingApi = {
  programs: () => http.get("/training-programs"),
  program: (id) => http.get(`/training-programs/${id}`),
  createProgram: (body) => http.post("/training-programs", body),
  updateProgram: (id, body) => http.put(`/training-programs/${id}`, body),
  cancelProgram: (id) => http.patch(`/training-programs/${id}/cancel`),
  deleteProgram: (id) => http.del(`/training-programs/${id}`),

  enrol: (employeeId, programId) => http.post("/training-enrollments", { employeeId, programId }),
  selfEnrol: (programId) => http.post("/training-enrollments/self", { programId }),
  myEnrollments: () => http.get("/training-enrollments/my"),
  byEmployee: (empId) => http.get(`/training-enrollments/employee/${empId}`),
  byProgram: (progId) => http.get(`/training-enrollments/program/${progId}`),
  complete: (id) => http.patch(`/training-enrollments/${id}/complete`),
  drop: (id) => http.del(`/training-enrollments/${id}`),
};

// /api/attendance
export const AttendanceApi = {
  log: (body) => http.post("/attendance", body), // { employeeId, date, checkInTime, checkOutTime, status }
  checkIn: () => http.post("/attendance/check-in"),
  checkOut: () => http.post("/attendance/check-out"),
  mine: () => http.get("/attendance/my"),
  byEmployee: (empId) => http.get(`/attendance/employee/${empId}`),
  byDepartment: (deptId) => http.get(`/attendance/department/${deptId}`),
  correct: (id, body) => http.put(`/attendance/${id}`, body), // overrideReason required
  remove: (id) => http.del(`/attendance/${id}`),
};

// /api/leave
export const LeaveApi = {
  apply: (body) => http.post("/leave", body), // { leaveType, startDate, endDate, reason }
  mine: () => http.get("/leave/my"),
  balance: () => http.get("/leave/balance"),
  pending: () => http.get("/leave/pending"),
  byEmployee: (empId) => http.get(`/leave/employee/${empId}`),
  get: (id) => http.get(`/leave/${id}`),
  approve: (id, comment) => http.patch(`/leave/${id}/approve`, comment ? { comment } : undefined),
  reject: (id, comment) => http.patch(`/leave/${id}/reject`, comment ? { comment } : undefined),
  cancel: (id) => http.del(`/leave/${id}`),
};

// /api/payroll
export const PayrollApi = {
  generate: (body) => http.post("/payroll/generate", body),
  list: () => http.get("/payroll"),
  mine: () => http.get("/payroll/my"),
  byEmployee: (empId) => http.get(`/payroll/employee/${empId}`),
  get: (id) => http.get(`/payroll/${id}`),
  update: (id, body) => http.put(`/payroll/${id}`, body), // DRAFT only
  finalize: (id) => http.patch(`/payroll/${id}/finalize`),
  pay: (id) => http.patch(`/payroll/${id}/pay`),
  void: (id) => http.del(`/payroll/${id}`),
  salaries: () => http.get("/payroll/salaries"),
  salary: (employeeId) => http.get(`/payroll/salaries/${employeeId}`),
  setSalary: (employeeId, baseSalary) => http.put(`/payroll/salaries/${employeeId}`, { baseSalary }),
};

// /api/performance
export const PerformanceApi = {
  create: (body) => http.post("/performance", body), // { employeeId, feedback, rating, reviewDate }
  mine: () => http.get("/performance/my"),
  byEmployee: (empId) => http.get(`/performance/employee/${empId}`),
  byTeam: (deptId) => http.get(`/performance/team/${deptId}`),
  update: (id, body) => http.put(`/performance/${id}`, body),
  remove: (id) => http.del(`/performance/${id}`),
};

// /api/notifications
export const NotificationsApi = {
  list: () => http.get("/notifications"),
  unread: () => http.get("/notifications/unread"),
  unreadCount: () => http.get("/notifications/unread-count", undefined, { quiet: true }), // { unread }
  markRead: (id) => http.patch(`/notifications/${id}/read`),
  markAllRead: () => http.patch("/notifications/read-all"),
  outbox: (limit = 200) => http.get("/notifications/outbox", { limit }), // IT_ADMIN: queued/sent/failed emails
  retry: (id) => http.patch(`/notifications/outbox/${id}/retry`), // IT_ADMIN: re-queue a FAILED email
};

// /api/activity-logs — IT_ADMIN
export const ActivityApi = {
  recent: (limit = 100) => http.get("/activity-logs", { limit }),
  byUser: (userId) => http.get(`/activity-logs/user/${userId}`),
  byAction: (action) => http.get(`/activity-logs/action/${encodeURIComponent(action)}`),
};

// /api/dashboard — COMPANY_DIRECTOR, HR_MANAGER
export const DashboardApi = {
  summary: () => http.get("/dashboard/summary"),
  department: (deptId) => http.get(`/dashboard/department/${deptId}`),
};

// /api/reports
export const ReportsApi = {
  headcount: () => http.get("/reports/headcount"),
  attendance: (start, end) => http.get("/reports/attendance", { start, end }),
  leave: (year) => http.get("/reports/leave", { year }),
  payroll: (start, end) => http.get("/reports/payroll", { start, end }),
  training: () => http.get("/reports/training"),
  performance: () => http.get("/reports/performance"),
  recruitment: () => http.get("/reports/recruitment"),
};
