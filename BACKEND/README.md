# EMS Backend — LankaTech Services (SE2030 Group 10)

Spring Boot 3 · Java 17 · plain JDBC (no JPA, no `JdbcTemplate`) · no Lombok · MySQL 8.
Built from `EMS_Backend_Master_Guide.md`.

```
Controller  (@RestController)  — HTTP in/out only
   ↓
Service     (@Service)         — business rules + validation
   ↓
DAO         (interface + impl) — hand-written SQL over plain JDBC (AbstractJdbcDao)
   ↓
MySQL
```

## 1. Run it

Prerequisites: **JDK 17+**, **Maven 3.8+** (or IntelliJ, which bundles Maven), **MySQL 8**.

1. Open MySQL Workbench and run `src/main/resources/schema.sql`
   (it drops and recreates `ems_db` and inserts seed data).
2. Check the MySQL username/password in `src/main/resources/application.properties`
   (default `root` / `1234`; or set `DB_USERNAME` / `DB_PASSWORD` environment variables).
3. Start the app: run `EmsApplication` in IntelliJ, or `mvn spring-boot:run`.
4. On first start you'll see `Set the default password 'password123' for 6 seed user(s)` —
   `DataInitializer` replaces the placeholder hashes in the seed data with real BCrypt hashes.

Unit tests (no database needed): `mvn test`.

## 2. Seed accounts (password for all: `password123`)

| user_id | Email | Role | Linked employee |
|---|---|---|---|
| 1 | hr@lankatech.lk | HR_MANAGER | — |
| 2 | admin@lankatech.lk | IT_ADMIN | — |
| 3 | director@lankatech.lk | COMPANY_DIRECTOR | — |
| 4 | payroll@lankatech.lk | PAYROLL_EXECUTIVE | — |
| 5 | supervisor@lankatech.lk | DEPT_SUPERVISOR | #2 Kasun Wickramasinghe (Engineering) |
| 6 | employee@lankatech.lk | EMPLOYEE | #1 Tharindu Silva (Engineering) |

Departments: #1 Human Resources, #2 Engineering. Positions: #1 HR Manager, #2 Software Engineer, #3 Engineering Supervisor.

## 3. Authentication

Session-based Spring Security with BCrypt. Two ways to authenticate:

- **Basic Auth** (easiest in Postman): Authorization tab → Basic Auth → email + password.
- **Session login**: `POST /api/auth/login` with `{ "email": "...", "password": "..." }` — the `JSESSIONID` cookie is then used automatically.

A supervisor's "team" is the department of the employee record linked to their user account.
Self-service features (leave, check-in, payslips, …) need the user to be linked to an employee record
(`PATCH /api/users/{id}/link-employee/{employeeId}` as IT Admin).

All errors come back as JSON: `{ "timestamp", "status", "error", "message" }`.

## 4. Endpoints

### Auth — `/api/auth`
| Method | Path | Who |
|---|---|---|
| POST | `/register` | public (always creates an EMPLOYEE account; an optional `role` is recorded as an access request for IT Admins) |
| POST | `/login` | public |
| POST | `/logout` | public |
| GET | `/me` | logged in |

### Users — `/api/users` (IT_ADMIN)
`GET /` · `GET /{id}` · `GET /role/{role}` · `POST /` (any role) · `PATCH /{id}/role` `{ "role" }` · `PATCH /{id}/link-employee/{employeeId}` · `PATCH /{id}/password` `{ "newPassword" }`

### Employees — `/api/employees`
| Method | Path | Who |
|---|---|---|
| POST | `/` | HR |
| GET | `/` | HR, Supervisor, Director, Payroll |
| GET | `/me` · PUT `/me` (phone, address) | the employee |
| GET | `/{id}` | HR/Director/Payroll: any · Supervisor: own team · Employee: self |
| GET | `/department/{deptId}` | HR, Director, Supervisor (own dept) |
| PUT | `/{id}` (partial update) | HR |
| DELETE | `/{id}` (→ INACTIVE) | HR |

### Departments — `/api/departments` and positions — `/api/positions`
GET endpoints: any logged-in user. POST / PUT / DELETE: HR.
Departments: `DELETE` deactivates (refused while employees are assigned). Positions: `GET /department/{deptId}` too.

### Recruitment (HR only)
`/api/vacancies`: `POST /` · `GET /?status=OPEN` · `GET /{id}` · `PUT /{id}` · `PATCH /{id}/close` · `PATCH /{id}/status` · `DELETE /{id}`

`/api/applications`: `POST /` · `GET /` · `GET /{id}` · `GET /vacancy/{vacancyId}` · `PATCH /{id}/status` · `DELETE /{id}` (→ WITHDRAWN)

Setting status to **HIRED** creates an Employee (status PROBATION, vacancy's department) in one transaction; the response contains `createdEmployee`.

### Training
`/api/training-programs`: `POST` (HR) · `GET /` · `GET /{id}` · `PUT /{id}` (HR) · `PATCH /{id}/cancel` (HR) · `DELETE /{id}` (HR)

`/api/training-enrollments`: `POST /` `{ employeeId, programId }` (HR, Supervisor-own team) · `POST /self` `{ programId }` · `GET /my` · `GET /employee/{empId}` · `GET /program/{progId}` (HR, Sup) · `PATCH /{id}/complete` (HR, Sup) · `DELETE /{id}` (→ DROPPED)

### Attendance — `/api/attendance`
`POST /` (Supervisor-own team, HR) · `POST /check-in` · `POST /check-out` · `GET /my` · `GET /employee/{empId}` · `GET /department/{deptId}` (Sup, HR, Payroll, Director) · `PUT /{id}` (Sup, HR — `overrideReason` required) · `DELETE /{id}` (HR)

### Leave — `/api/leave`
`POST /` (for yourself) · `GET /my` · `GET /balance` · `GET /pending` (Sup, HR) · `GET /employee/{empId}` · `GET /{id}` · `PATCH /{id}/approve` · `PATCH /{id}/reject` (Sup-own team, HR; optional `{ "comment" }`) · `DELETE /{id}` (cancel own)

Rules: always starts PENDING; start date today or later; no overlapping requests; yearly entitlement ANNUAL 14, CASUAL 7, SICK 7, MATERNITY 84, OTHER unlimited; you can't approve your own request.

### Payroll — `/api/payroll`
`POST /generate` (Payroll) · `GET /` (Payroll, Director) · `GET /my` · `GET /employee/{empId}` · `GET /{id}` · `PUT /{id}` (Payroll, DRAFT only) · `PATCH /{id}/finalize` · `PATCH /{id}/pay` · `DELETE /{id}` (→ VOIDED)

Rules: refused (409) while PENDING leave exists in the period; one active record per employee per period; if `overtimeHours` is omitted it is calculated from attendance (time beyond 8h/day); employees only see FINALIZED/PAID payslips.

### Performance — `/api/performance`
`POST /` (Supervisor-own team) · `GET /my` · `GET /employee/{empId}` (Sup, HR, Director) · `GET /team/{deptId}` · `PUT /{id}` (author) · `DELETE /{id}` (author or HR)

### Notifications — `/api/notifications` (own)
`GET /` · `GET /unread` · `GET /unread-count` · `PATCH /{id}/read` · `PATCH /read-all`

### Activity log — `/api/activity-logs` (IT_ADMIN)
`GET /?limit=100` · `GET /user/{userId}` · `GET /action/{action}`

### Dashboard & reports
`/api/dashboard/summary`, `/api/dashboard/department/{deptId}` (Director, HR)

`/api/reports/headcount`, `/attendance?start&end`, `/leave?year`, `/payroll?start&end`, `/training`, `/performance`, `/recruitment`
(Director/HR; Payroll can read attendance, leave, payroll; dates default to the current month)

## 5. Postman walkthrough (guide §28, adjusted to the seed data)

1. **HR** `GET /api/auth/me`
2. **HR** `POST /api/departments` `{ "name": "Marketing", "description": "Marketing and communications department" }` → department #3
3. **HR** `POST /api/vacancies` `{ "title": "Marketing Manager", "departmentId": 3, "requirements": "5+ years marketing experience", "deadline": "2026-11-01" }`
4. **HR** `POST /api/applications` `{ "vacancyId": 1, "candidateName": "Amaya Rathnayake", "candidateEmail": "amaya@gmail.com", "candidatePhone": "0771234567", "candidateNic": "200012345678", "appliedDate": "2026-09-15" }`
5. **HR** `PATCH /api/applications/1/status` `{ "status": "HIRED" }` → employee #3 created (PROBATION)
6. **Supervisor** `POST /api/attendance` `{ "employeeId": 1, "date": "2026-09-15", "checkInTime": "08:45", "checkOutTime": "17:30", "status": "PRESENT" }`
7. **Employee** `POST /api/leave` `{ "leaveType": "ANNUAL", "startDate": "2026-10-01", "endDate": "2026-10-03", "reason": "Family event" }`
8. **Supervisor** `PATCH /api/leave/1/approve`
9. **Payroll** `POST /api/payroll/generate` `{ "employeeId": 1, "payPeriodStart": "2026-09-01", "payPeriodEnd": "2026-09-30", "baseSalary": 75000.00, "overtimeHours": 10, "deductions": 5000 }` → net pay 77031.25
10. **Payroll** `PATCH /api/payroll/1/finalize` → **Employee** `GET /api/payroll/my` and `GET /api/notifications`
11. **Director** `GET /api/dashboard/summary`
12. **IT Admin** `GET /api/activity-logs`

## 6. Differences from the guide (and why)

- **Seed passwords**: the guide's placeholder hashes can't log in. `schema.sql` uses `CHANGE_ME_ON_STARTUP` and `config/DataInitializer` hashes `password123` at startup.
- **Seed data** adds an EMPLOYEE user and links the supervisor/employee accounts to employee records, so team-scope and self-service features work immediately.
- **`/api/auth/register`** is public but always creates EMPLOYEE accounts (otherwise anyone could self-register as IT_ADMIN). Other roles: `POST /api/users`.
  If the request includes a management `role`, it is treated as an **access request**: logged as `REQUEST_ROLE`, and every IT Admin gets an `ACCESS_REQUEST` notification. The admin grants it with `PATCH /api/users/{id}/role` (the frontend's Users & access page shows open requests with a Grant button).
- **`POST /api/auth/login`** is implemented (the guide listed it but had no endpoint).
- **Security rules**: the guide's URL rules contradicted its endpoint tables (e.g. `/api/leave/approve/**` vs `/api/leave/{id}/approve`, employees blocked from their own payslips). Coarse rules stay in `SecurityConfig`; per-endpoint rules are `@PreAuthorize`; record-level rules (own record / own team) are in `security/AccessGuard`.
- **`@Transactional` really works**: `AbstractJdbcDao` wraps the `DataSource` in `TransactionAwareDataSourceProxy`, so the hire flow, leave approval and payroll generation commit or roll back as one unit.
- **Exception handler** also maps `AccessDeniedException` → 403, bad JSON → 400, MySQL duplicate key → 409 and foreign-key errors → 400/409 (the guide's catch-all turned these into 500).
- **Notifications** go to the employee's *user* account (the guide passed an employee id as a user id).
- **JDBC URL** has no `serverTimezone=UTC`, so `DATETIME` values aren't shifted on a local MySQL.
- Extra pieces the guide mentioned but didn't define: `JobPositionController`, `UserController`, `ReportController`, `NotificationController`, leave balance, employee check-in/out, self-enrolment, payslip `pay` step.
