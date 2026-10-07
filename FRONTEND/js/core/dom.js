// Tiny DOM toolkit. Every piece of UI is built with h(), which only ever creates
// text nodes for strings — backend data is never parsed as HTML.

const PROP_KEYS = new Set(["value", "checked", "disabled", "selected", "hidden", "indeterminate", "multiple", "required", "readOnly"]);

export function h(tag, props, ...children) {
  const el = document.createElement(tag);

  if (props) {
    for (const [key, val] of Object.entries(props)) {
      if (val == null || val === false) continue;

      if (key === "class") {
        el.className = Array.isArray(val) ? val.filter(Boolean).join(" ") : val;
      } else if (key === "style" && typeof val === "object") {
        Object.assign(el.style, val);
      } else if (key === "dataset") {
        Object.assign(el.dataset, val);
      } else if (key === "ref") {
        val(el);
      } else if (key.startsWith("on") && typeof val === "function") {
        el.addEventListener(key.slice(2).toLowerCase(), val);
      } else if (PROP_KEYS.has(key)) {
        el[key] = val;
      } else {
        el.setAttribute(key, val === true ? "" : String(val));
      }
    }
  }

  append(el, children);
  return el;
}

export function append(el, children) {
  for (const child of children.flat(Infinity)) {
    if (child == null || child === false || child === true) continue;
    el.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
  return el;
}

export function replaceChildren(el, ...children) {
  el.replaceChildren();
  return append(el, children);
}

export const icon = (name, { fill = false, size, cls } = {}) =>
  h("span", { class: ["icon", fill && "icon--fill", size && `icon--${size}`, cls], "aria-hidden": "true" }, name);

export const fragment = (...children) => append(document.createDocumentFragment(), children);

export function debounce(fn, wait = 200) {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), wait);
  };
}

// ---- Ripple: one delegated listener for every .state element ----
export function installRipple(root = document) {
  root.addEventListener("pointerdown", (event) => {
    const target = event.target.closest(".state");
    if (!target || target.disabled || event.button !== 0) return;

    const rect = target.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height) * 2;
    const ripple = document.createElement("span");
    ripple.className = "ripple";
    ripple.style.width = ripple.style.height = `${size}px`;
    ripple.style.left = `${event.clientX - rect.left - size / 2}px`;
    ripple.style.top = `${event.clientY - rect.top - size / 2}px`;
    target.append(ripple);
    ripple.addEventListener("animationend", () => ripple.remove(), { once: true });
  });
}
