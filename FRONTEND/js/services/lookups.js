// Cached reference data shared across pages (departments, positions, employees).
// Loaders are role-aware so a page never calls an endpoint the user can't use.

import { DepartmentsApi, EmployeesApi, PositionsApi } from "./api.js";
import { session } from "../core/session.js";
import { can, hasRole, isLinked, ROLES } from "../core/permissions.js";
import { fullName } from "../core/format.js";

const cache = new Map();

function cached(key, loader) {
  if (!cache.has(key)) {
    cache.set(
      key,
      loader().catch((err) => {
        cache.delete(key);
        throw err;
      })
    );
  }
  return cache.get(key);
}

export const lookups = {
  departments: () => cached("departments", DepartmentsApi.list),

  positions: () => cached("positions", PositionsApi.list),

  // The signed-in user's own employee record (null when the account isn't linked)
  myEmployee: () =>
    cached("myEmployee", async () => (isLinked(session.user) ? EmployeesApi.me() : null)),

  // Everyone this user may see
  employees: () =>
    cached("employees", async () => {
      const user = session.user;
      if (can(user, "listEmployees")) return EmployeesApi.list();
      const me = await lookups.myEmployee();
      return me ? [me] : [];
    }),

  // A supervisor's team = employees in the department of their linked record.
  // For HR it is everyone; others have no team.
  team: () =>
    cached("team", async () => {
      const user = session.user;
      if (hasRole(user, ROLES.HR)) return EmployeesApi.list();
      if (hasRole(user, ROLES.SUPERVISOR)) {
        const me = await lookups.myEmployee();
        return me?.departmentId ? EmployeesApi.byDepartment(me.departmentId) : [];
      }
      return [];
    }),

  async maps() {
    const [departments, positions, employees] = await Promise.all([
      lookups.departments().catch(() => []),
      lookups.positions().catch(() => []),
      lookups.employees().catch(() => []),
    ]);
    return {
      departments: new Map(departments.map((d) => [d.departmentId, d])),
      positions: new Map(positions.map((p) => [p.positionId, p])),
      employees: new Map(employees.map((e) => [e.employeeId, e])),
    };
  },

  invalidate(...keys) {
    if (!keys.length) cache.clear();
    keys.forEach((key) => cache.delete(key));
  },
};

export const employeeLabel = (map, id) => {
  const emp = map.get(Number(id));
  return emp ? fullName(emp) : `Employee #${id}`;
};

export const departmentLabel = (map, id) => (id == null ? "—" : map.get(Number(id))?.name ?? `Department #${id}`);

export const positionLabel = (map, id) => (id == null ? "—" : map.get(Number(id))?.title ?? `Position #${id}`);
