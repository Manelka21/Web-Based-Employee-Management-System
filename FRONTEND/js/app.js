// Bootstrap: restores the session, starts the router, owns the shell lifecycle
// and renders each page into the content panel.

import { h, installRipple } from "./core/dom.js";
import { bus, EVENTS } from "./core/events.js";
import { session } from "./core/session.js";
import { createRouter, navigate, parseHash, buildHash } from "./core/router.js";
import { AuthApi } from "./services/api.js";
import { lookups } from "./services/lookups.js";
import { routes, routeOn } from "./routes.js";
import { createShell } from "./layout/shell.js";
import { loadingState, errorState } from "./components/feedback.js";
import { toast, toastError } from "./components/snackbar.js";
import { closeMenus } from "./components/menu.js";
import { notFoundPage, forbiddenPage, featureOffPage } from "./pages/system.js";

const root = document.getElementById("root");
const progress = document.getElementById("progress");

let shell = null;
let renderToken = 0;
let cleanups = [];

// ---- Global loading bar (delayed so fast requests don't flicker) ----
let progressTimer;
bus.on(EVENTS.HTTP_BUSY, (count) => {
  clearTimeout(progressTimer);
  if (count > 0) progressTimer = setTimeout(() => progress.classList.add("is-active"), 150);
  else progress.classList.remove("is-active");
});

// ---- Session expiry ----
bus.on(EVENTS.UNAUTHORIZED, () => {
  if (!session.isAuthenticated) return;
  const { path, query } = parseHash();
  endSession();
  toast("Your session has ended. Please sign in again.");
  navigate("/login", { next: buildHash(path, query).slice(1) });
});

// The API answered 403 PASSWORD_CHANGE_REQUIRED (e.g. an admin reset the password mid-session)
bus.on(EVENTS.PASSWORD_CHANGE_REQUIRED, () => {
  if (!session.user || session.user.mustChangePassword) return;
  session.set({ user: { ...session.user, mustChangePassword: true } });
  navigate("/change-password");
});

function endSession() {
  session.clear();
  lookups.invalidate();
  shell?.destroy();
  shell = null;
}

async function signOut() {
  try {
    await AuthApi.logout();
  } catch (err) {
    toastError(err);
  }
  endSession();
  navigate("/login");
}

export async function completeSignIn(user, next) {
  lookups.invalidate();
  session.set({ user });
  navigate(next && next.startsWith("/") && !next.startsWith("/login") ? next : "/");
}

function runCleanups() {
  cleanups.forEach((fn) => {
    try {
      fn();
    } catch (err) {
      console.error(err);
    }
  });
  cleanups = [];
}

async function renderPage(route, target, ctx) {
  const token = ++renderToken;
  runCleanups();
  closeMenus();
  target.replaceChildren(loadingState());

  try {
    const module = await route.load();
    const view = await module.default(ctx);
    if (token !== renderToken) return;
    target.replaceChildren(view);
    target.scrollTop = 0;
  } catch (err) {
    if (token !== renderToken) return;
    console.error(err);
    target.replaceChildren(h("div", { class: "page" }, errorState(err, { onRetry: () => router.refresh() })));
  }
}

const router = createRouter({
  routes,
  onRoute: ({ route, params, query, path }) => {
    const user = session.user;
    const ctx = {
      params,
      query,
      path,
      user,
      navigate,
      reload: () => router.refresh(),
      onCleanup: (fn) => cleanups.push(fn),
      completeSignIn,
    };

    // Public pages (sign in / register) render full-screen without the shell
    if (route?.public) {
      if (user && route.guestOnly) return navigate("/");
      shell?.destroy();
      shell = null;
      document.title = `${route.title} · LankaTech EMS`;
      return renderPage(route, root, ctx);
    }

    if (!user) {
      const next = buildHash(path, query).slice(1);
      return navigate("/login", path === "/" ? undefined : { next });
    }

    // After an admin reset the API only accepts a password change, so go straight there
    if (user.mustChangePassword && path !== "/change-password") {
      return navigate("/change-password");
    }
    if (route?.bare) {
      if (route.path === "/change-password" && !user.mustChangePassword) return navigate("/");
      shell?.destroy();
      shell = null;
      document.title = `${route.title} · LankaTech EMS`;
      return renderPage(route, root, ctx);
    }

    if (!shell) {
      shell = createShell({ user, onSignOut: signOut });
      root.replaceChildren(shell.el);
    }
    shell.setActive(route);

    if (!route) {
      runCleanups();
      document.title = "Page not found · LankaTech EMS";
      return shell.outlet.replaceChildren(notFoundPage());
    }
    // Switched off in js/features.js (switch-features.bat)
    if (!routeOn(route)) {
      runCleanups();
      document.title = "Turned off · LankaTech EMS";
      return shell.outlet.replaceChildren(featureOffPage(route.title));
    }
    if (route.allow && !route.allow(user)) {
      runCleanups();
      document.title = "No access · LankaTech EMS";
      return shell.outlet.replaceChildren(forbiddenPage());
    }

    document.title = `${route.title} · LankaTech EMS`;
    renderPage(route, shell.outlet, ctx);
  },
});

async function boot() {
  installRipple();
  try {
    const user = await AuthApi.me();
    session.set({ user });
  } catch (err) {
    // 401 simply means "not signed in"; anything else is worth surfacing
    if (err.status !== 401) console.warn("Session check failed:", err.message);
  }
  router.start();
}

boot();
