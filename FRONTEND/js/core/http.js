// HTTP client for the EMS Spring Boot API.
//
// - Session auth: the backend sets a JSESSIONID cookie on POST /auth/login, so every
//   request is sent with credentials: "include".
// - CSRF: state-changing requests carry the token from GET /auth/csrf in the header the
//   backend names (X-CSRF-TOKEN). If the token has expired the request is retried once.
// - Errors: the backend always answers { timestamp, status, error, message }; they are
//   normalised into ApiError so pages can show `err.message` directly.

import { bus, EVENTS } from "./events.js";

const BASE_URL = (window.EMS_CONFIG?.apiBaseUrl || "http://localhost:8080/api").replace(/\/$/, "");
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);
// Endpoints the backend exempts from CSRF (they run before a session exists)
const CSRF_EXEMPT = ["/auth/login", "/auth/register", "/auth/logout", "/auth/forgot-password"];

export class ApiError extends Error {
  constructor(status, message, body) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

let inFlight = 0;
function track(delta) {
  inFlight = Math.max(0, inFlight + delta);
  bus.emit(EVENTS.HTTP_BUSY, inFlight);
}

function buildUrl(path, query) {
  const url = new URL(BASE_URL + path);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") url.searchParams.set(key, value);
    }
  }
  return url.toString();
}

// ---- CSRF token (one per session) ----
let csrf = null; // { headerName, token }
let csrfLoading = null;

async function csrfHeader(forceRefresh = false) {
  if (forceRefresh) csrf = null;
  if (!csrf) {
    csrfLoading ??= fetch(buildUrl("/auth/csrf"), { credentials: "include", headers: { Accept: "application/json" } })
      .then((res) => (res.ok ? res.json() : null))
      .catch(() => null)
      .finally(() => (csrfLoading = null));
    csrf = await csrfLoading;
  }
  return csrf ? { [csrf.headerName]: csrf.token } : {};
}

// The session (and its token) changes on sign-in and sign-out
export function resetCsrf() {
  csrf = null;
}

async function request(method, path, { body, query, quiet = false } = {}, retried = false) {
  const init = {
    method,
    credentials: "include",
    headers: { Accept: "application/json" },
  };
  if (body !== undefined) {
    init.headers["Content-Type"] = "application/json";
    init.body = JSON.stringify(body);
  }
  if (!SAFE_METHODS.has(method) && !CSRF_EXEMPT.includes(path)) {
    Object.assign(init.headers, await csrfHeader(retried));
  }

  if (!quiet) track(1);
  let response;
  try {
    response = await fetch(buildUrl(path, query), init);
  } catch {
    throw new ApiError(0, `Can't reach the server at ${BASE_URL}. Check that the backend is running.`);
  } finally {
    if (!quiet) track(-1);
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    // Expired/missing CSRF token: fetch a fresh one and try once more
    if (response.status === 403 && data?.error === "CSRF" && !retried) {
      return request(method, path, { body, query, quiet }, true);
    }
    // After an admin password reset, the user must pick a new password first
    if (response.status === 403 && data?.error === "PASSWORD_CHANGE_REQUIRED") {
      bus.emit(EVENTS.PASSWORD_CHANGE_REQUIRED);
    }

    const message =
      (data && typeof data === "object" && data.message) ||
      (typeof data === "string" && data) ||
      response.statusText ||
      "Something went wrong";

    // A lost session anywhere except the auth endpoints sends the user back to sign in
    if (response.status === 401 && !path.startsWith("/auth/")) {
      resetCsrf();
      bus.emit(EVENTS.UNAUTHORIZED);
    }
    throw new ApiError(response.status, message, data);
  }

  if (path === "/auth/login" || path === "/auth/logout") resetCsrf();
  return data;
}

export const http = {
  get: (path, query, opts) => request("GET", path, { query, ...opts }),
  post: (path, body) => request("POST", path, { body }),
  put: (path, body) => request("PUT", path, { body }),
  patch: (path, body) => request("PATCH", path, { body }),
  del: (path) => request("DELETE", path),
};

export const API_BASE_URL = BASE_URL;
