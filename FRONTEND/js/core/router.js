// Hash router: "#/employees/12?tab=leave".
// Hash routing keeps the app deployable as plain static files with no server rewrites.

function compile(pattern) {
  const keys = [];
  const source = pattern
    .replace(/\/+$/, "")
    .replace(/[.+*?^${}()|[\]\\]/g, "\\$&")
    .replace(/:(\w+)/g, (_, key) => {
      keys.push(key);
      return "([^/]+)";
    });
  return { regex: new RegExp(`^${source || ""}/?$`), keys };
}

export function parseHash(hash = window.location.hash) {
  const raw = hash.replace(/^#/, "") || "/";
  const [pathPart, queryPart = ""] = raw.split("?");
  const path = pathPart.startsWith("/") ? pathPart : `/${pathPart}`;
  const query = Object.fromEntries(new URLSearchParams(queryPart));
  return { path, query };
}

export function buildHash(path, query = {}) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== "") params.set(key, value);
  }
  const qs = params.toString();
  return `#${path}${qs ? `?${qs}` : ""}`;
}

export function navigate(path, query) {
  const target = buildHash(path, query);
  if (window.location.hash === target) {
    window.dispatchEvent(new HashChangeEvent("hashchange"));
  } else {
    window.location.hash = target;
  }
}

// Update query params (tabs, filters) without triggering a re-render
export function setQuery(patch) {
  const { path, query } = parseHash();
  const next = { ...query, ...patch };
  history.replaceState(null, "", buildHash(path, next));
}

// Reads a one-shot ?action=... (e.g. from the Create menu) and removes it from the URL
export function takeAction(ctx) {
  const action = ctx.query.action;
  if (action) setQuery({ action: null });
  return action;
}

export function createRouter({ routes, onRoute }) {
  const compiled = routes.map((route) => ({ ...route, ...compile(route.path) }));

  function resolve() {
    const { path, query } = parseHash();
    for (const route of compiled) {
      const match = route.regex.exec(path);
      if (!match) continue;
      const params = Object.fromEntries(route.keys.map((key, i) => [key, decodeURIComponent(match[i + 1])]));
      onRoute({ route, params, query, path });
      return;
    }
    onRoute({ route: null, params: {}, query, path });
  }

  return {
    start() {
      window.addEventListener("hashchange", resolve);
      resolve();
    },
    refresh: resolve,
  };
}
