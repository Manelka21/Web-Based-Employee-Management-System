# EMS Frontend — LankaTech Services

A vanilla **HTML + CSS + JavaScript** web app for the Spring Boot EMS in `../BACKEND`.
It uses Material 3 design with Material Symbols icons, in light mode with the brand colours `#57B9FF` and `#90D5FF`.
There's no build step, no framework and no npm dependencies.

## 1. Run it

1. Start the backend (see `../BACKEND/README.md`). It runs on `http://localhost:8080`.
2. Serve this folder on **port 5173** (or 3000). These are the only origins the backend's CORS config accepts with cookies:

   ```bash
   node server.js
   ```

3. Open <http://localhost:5173> and sign in. The sign-in page has two portals on the same screen:
   **Employee** (`#/login`) for employee accounts, and **Admin & management** (`#/login?portal=staff`) for HR, supervisors, payroll, directors and IT administrators.
   Both call `POST /api/auth/login`. A portal only accepts its own roles, and points users with the wrong account to the other portal. All seed accounts use the password `password123`. The sign-in screen has one-tap demo accounts; turn them off in `js/config.js`.

The backend database must be up to date first (`migration_v2.sql` + `seed.sql`, see `../QA_REPORT.md`).

### Accounts and passwords

| Flow | What the user sees |
|---|---|
| **Create account** (`#/register`) | Only for existing employees: the work email **and NIC** must match their employee record. The account is linked to that record straight away. |
| **Request management access** (sign-in → Admin & management) | Same email + NIC check, plus the role wanted. The account starts as Employee; the IT admin grants the role. |
| **Forgot password?** (sign-in) | Asks the IT admins to reset it (there's no email reset link). The answer is the same whether or not the email exists. |
| **Choose a new password** (`#/change-password`) | Full-screen and unavoidable after an admin reset or a new admin-created account. The API refuses everything else until it's done. |
| **Change password** (My profile) | Current password + new password, any time. |
| **Lockout** | 5 wrong passwords lock the account for 15 minutes; the sign-in page says so. The IT admin can unlock it. |
| **Disabled account** | The IT admin can disable an account, and HR deactivating an employee disables theirs. Sign-in is refused, and open sessions end at the next click. |

Password rules (enforced in the form and the API): 8–72 characters, a letter and a number, not containing your email name.

The API address and other settings are in `js/config.js`.

## 2. How the frontend talks to the backend

| Concern | How it works |
|---|---|
| Authentication | `POST /api/auth/login` sets a `JSESSIONID` cookie. Every request is sent with `credentials: "include"` (`js/core/http.js`). On load, `GET /api/auth/me` restores the session. |
| Errors | The backend answers `{ timestamp, status, error, message }`. `http.js` turns this into an `ApiError`, and its `message` appears in dialogs and snackbars as-is. |
| Session expiry | Any 401 outside `/auth/*` signs the user out and returns them to sign-in, with a `next` link back. |
| CSRF | `POST/PUT/PATCH/DELETE` requests send the token from `GET /api/auth/csrf` in `X-CSRF-TOKEN`. If it expired (403 `CSRF`), `http.js` fetches a new one and retries once. |
| Forced password change | A 403 `PASSWORD_CHANGE_REQUIRED` from any endpoint sends the user to `#/change-password`. |
| Validation | `js/core/validators.js` mirrors the backend rules (names, Sri Lankan phone/NIC, email, password, dates, amounts). Field rules use `validate`; cross-field rules use `formDialog({ validate })`. Buttons are disabled while saving, so nothing is submitted twice. |
| Permissions | `js/core/permissions.js` mirrors `SecurityConfig` and `@PreAuthorize`, so the UI only shows actions the role can perform. Record-level rules (own team, own record) are still enforced by the API. |
| Endpoints | `js/services/api.js` has one object per controller and one method per endpoint. |

## 3. Architecture

```
index.html              App entry: fonts, icons, CSS, config, app.js
server.js               Zero-dependency static server (port 5173)
assets/logo.svg
css/
  tokens.css            Material 3 colour roles, type, shape, elevation, motion
  base.css              Reset, type scale, icons, utilities
  components.css        Buttons, fields, chips, cards, tables, tabs, dialogs, snackbar, menus…
  layout.css            App shell (top bar, drawer/rail, content panel), auth, responsive rules
js/
  config.js             Runtime settings (API URL, demo accounts, polling interval)
  app.js                Bootstrap: session restore, router, shell lifecycle, page rendering
  routes.js             URL → lazy page module, role guard, navigation entry, Create menu
  core/                 Framework-free foundations
    dom.js              h() hyperscript (text-only, XSS-safe), icons, ripple
    http.js             fetch wrapper, ApiError, loading-bar signal
    router.js           Hash router, query helpers
    session.js          Signed-in user store
    events.js           Pub/sub bus
    permissions.js      Roles and capabilities
    format.js           Dates, LKR currency, enum labels
    csv.js              Report export
    validators.js       Field rules shared by every form (mirror the backend), working-day counter
  services/
    api.js              Endpoint map (Auth, Users, Employees, Departments, Positions, Vacancies,
                        Applications, Training, Attendance, Leave, Payroll, Performance,
                        Notifications, Activity, Dashboard, Reports)
    lookups.js          Cached, role-aware reference data (departments, positions, employees, team)
  components/           Reusable UI: button, fields, dialog, table, tabs, menu, snackbar, status, page, feedback
  widgets/              Domain building blocks shared by pages (dialogs, table columns, self-service cards, notifications,
                        accounts.js = open access/password-reset requests for the IT admin)
  layout/shell.js       Top app bar with search, notifications and account menu; navigation drawer
  pages/                One module per screen; each default-exports (ctx) => Node
                        (auth/changePassword.js = forced password change; admin/outbox.js = email outbox)
```

**Page contract.** A page module default-exports `function (ctx)`, which returns a DOM node (or a Promise of one).
`ctx` contains `{ params, query, user, navigate, reload, onCleanup }`. Pages load data through `asyncSection()`, which handles the loading, error and retry states for each block independently.

## 4. Screens by role

| Screen | Employee | Supervisor | HR | Payroll | Director | IT Admin |
|---|---|---|---|---|---|---|
| Home (role-aware overview) | ● | ● | ● | ● | ● | ● |
| Notifications, My profile | ● | ● | ● | ● | ● | ● |
| Directory, employee detail | own | team | manage | view | view | — |
| Organization (departments, positions, **holidays**) | view | view | manage | view | view | view |
| Recruitment (vacancies, candidates, edit details, hire) | — | — | ● | — | — | — |
| Training | self-enrol | enrol team | manage | view | view | — |
| Performance | my feedback | record reviews | view, delete | — | view | — |
| Attendance | check in/out | log, correct team | log, correct, delete | view | view | — |
| Leave (working days, balance preview, upcoming holidays) | apply, cancel | approve team | approve | — | — | — |
| Payroll | payslips | payslips | payslips + **salaries** | generate → finalize → pay, **salaries** | view | — |
| Employee detail **salary card** | — | — | ● | ● | — | — |
| Reports | — | — | ● | attendance, leave, payroll | ● | — |
| Users & access (roles, links, **disable/enable, unlock, password-reset requests**), Activity log, **Email outbox** | — | — | — | — | — | ● |

Leave counts **working days**: weekends and the public holidays HR records under Organization → Holidays don't use up the balance. The apply dialog shows the count live and stops requests that exceed the balance. Maternity leave is offered only to employees recorded as female.

Self-service features need the user account to be linked to an employee record (Users & access → Link employee record).

## Feature switch (`switch-features.bat`)

Double-click `switch-features.bat` (or run `switch-features.bat on|off|status`) to turn the
non-core functions on or off, then refresh the browser. It only rewrites `js/features.js`;
the backend is never changed.

- **Always on:** Department management (Organization: departments, positions), Onboarding &
  recruitment, Training, plus Sign in, Home, Profile and Notifications.
- **Switchable:** Directory, Attendance, Leave (and the Holidays tab), Payroll, Performance,
  Reports, Users & access, Activity log, Email outbox.

Switched-off pages disappear from the menu, search and Create button, their cards and tabs are
hidden, and opening one by URL shows "This page is turned off". Deleting `js/features.js` turns
everything on.
