// Session store — the signed-in user (UserResponse from /auth/me) and, when the
// account is linked, their employee record. Views subscribe via EVENTS.SESSION.

import { bus, EVENTS } from "./events.js";

const state = {
  user: null,
  employee: null,
};

export const session = {
  get user() {
    return state.user;
  },

  get employee() {
    return state.employee;
  },

  get isAuthenticated() {
    return state.user != null;
  },

  set(patch) {
    Object.assign(state, patch);
    bus.emit(EVENTS.SESSION, { ...state });
  },

  clear() {
    state.user = null;
    state.employee = null;
    bus.emit(EVENTS.SESSION, { ...state });
  },
};
