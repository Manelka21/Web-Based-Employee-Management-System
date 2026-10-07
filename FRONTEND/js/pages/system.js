import { h } from "../core/dom.js";
import { emptyState } from "../components/feedback.js";

export const notFoundPage = () =>
  h(
    "div",
    { class: "page" },
    emptyState({
      icon: "explore_off",
      title: "This page doesn't exist",
      text: "The link may be broken or the page may have moved.",
      action: { label: "Go home", icon: "home", onClick: () => (location.hash = "#/") },
    })
  );

export const forbiddenPage = () =>
  h(
    "div",
    { class: "page" },
    emptyState({
      icon: "lock",
      title: "You don't have access to this page",
      text: "Your role doesn't include this area. If you think that's wrong, contact your IT administrator.",
      action: { label: "Go home", icon: "home", onClick: () => (location.hash = "#/") },
    })
  );

export const featureOffPage = (title) =>
  h(
    "div",
    { class: "page" },
    emptyState({
      icon: "toggle_off",
      title: `${title || "This page"} is turned off`,
      text: "This function has been switched off for this installation. Run switch-features.bat in the FRONTEND folder to turn it back on.",
      action: { label: "Go home", icon: "home", onClick: () => (location.hash = "#/") },
    })
  );
