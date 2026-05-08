import { describe, expect, it } from "vitest";
import { buildPath, normalizeAppBase, resolveAppHref } from "./route";

describe("route base helpers", () => {
  it("normalizes root and equity base paths", () => {
    expect(normalizeAppBase("/")).toBe("");
    expect(normalizeAppBase("/equity/")).toBe("/equity");
  });

  it("resolves browser hrefs under the equity base path", () => {
    expect(resolveAppHref("/", "/equity/")).toBe("/equity/");
    expect(resolveAppHref("/index", "/equity/")).toBe("/equity/index");
    expect(resolveAppHref("/landing", "/equity/")).toBe("/equity/landing");
  });

  it("preserves query parameters when prefixing browser hrefs", () => {
    const jointEntryPath = buildPath("/joint-entry", {
      token: "joint-token-001",
      scene: "push",
      productCode: "PROD-001",
    });

    expect(resolveAppHref(jointEntryPath, "/equity/")).toBe(
      "/equity/joint-entry?token=joint-token-001&scene=push&productCode=PROD-001",
    );
  });
});
