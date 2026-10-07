// Minimal pub/sub used for cross-cutting signals (session changes, HTTP activity,
// unauthorised responses, notification counts).

const listeners = new Map();

export const bus = {
  on(event, handler) {
    if (!listeners.has(event)) listeners.set(event, new Set());
    listeners.get(event).add(handler);
    return () => listeners.get(event)?.delete(handler);
  },

  emit(event, payload) {
    listeners.get(event)?.forEach((handler) => {
      try {
        handler(payload);
      } catch (err) {
        console.error(`[bus] ${event} handler failed`, err);
      }
    });
  },
};

export const EVENTS = {
  HTTP_BUSY: "http:busy",
  UNAUTHORIZED: "http:unauthorized",
  SESSION: "session:change",
  NOTIFICATIONS: "notifications:changed",
  PASSWORD_CHANGE_REQUIRED: "auth:password-change-required",
};
