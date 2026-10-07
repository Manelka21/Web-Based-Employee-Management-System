// Runtime configuration. Loaded as a classic script before the app so it can be
// edited without touching any module.
//
// The backend's CORS rules (BACKEND/.../config/WebConfig.java) only accept
// http://localhost:3000 and http://localhost:5173 with credentials, so serve this
// folder from one of those origins (see server.js / README.md).
window.EMS_CONFIG = {
  apiBaseUrl: "http://localhost:8080/api",

  // Shows one-tap seed accounts on the sign-in screen. Turn off outside development.
  showDemoAccounts: true,

  // How often the notification badge refreshes (ms)
  notificationPollMs: 60000,
};
