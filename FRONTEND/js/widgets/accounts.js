// Account-request helpers for the IT administrator (Users & access, Home).
// Requests are recorded by the backend in the activity log:
//   REQUEST_ROLE            "Requested HR_MANAGER access for x@y"   (by the requester)
//   PASSWORD_RESET_REQUEST  "Password reset requested for x@y"       (by the requester)
//   RESET_PASSWORD          "Reset password of user #12"             (by the admin)

import { ActivityApi } from "../services/api.js";

const requestedRoleOf = (log) => /Requested (\w+) access/.exec(log.details || "")?.[1];
const resetTargetOf = (log) => Number(/user #(\d+)/.exec(log.details || "")?.[1]);
const byTime = (a, b) => String(a.timestamp).localeCompare(String(b.timestamp));

/**
 * Open requests, keyed by userId:
 *   roles:  userId -> requested role (only while the user doesn't have it yet)
 *   resets: userId -> request timestamp (only until an admin resets that password)
 */
export async function openAccountRequests(users) {
  const [roleLogs, resetRequests, resetsDone] = await Promise.all([
    ActivityApi.byAction("REQUEST_ROLE").catch(() => []),
    ActivityApi.byAction("PASSWORD_RESET_REQUEST").catch(() => []),
    ActivityApi.byAction("RESET_PASSWORD").catch(() => []),
  ]);
  const byId = new Map(users.map((u) => [u.userId, u]));

  const roles = new Map();
  [...roleLogs].sort(byTime).forEach((log) => {
    const role = requestedRoleOf(log);
    const user = byId.get(log.userId);
    if (!role || !user) return;
    if (user.role === role) roles.delete(user.userId);
    else roles.set(user.userId, role);
  });

  const resets = new Map();
  [...resetRequests].sort(byTime).forEach((log) => {
    const user = byId.get(log.userId);
    if (!user) return;
    const handled = resetsDone.some((done) => resetTargetOf(done) === user.userId && String(done.timestamp) > String(log.timestamp));
    if (handled) resets.delete(user.userId);
    else resets.set(user.userId, log.timestamp);
  });

  return { roles, resets };
}
