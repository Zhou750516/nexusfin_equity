import { describe, expect, it } from "vitest";
import { readRedirectHint, shouldRecoverJointSession } from "./api.logic";

describe("readRedirectHint", () => {
  it("reads redirect hint from api payload data", () => {
    expect(readRedirectHint({
      code: 401,
      message: "Missing auth cookie",
      data: {
        redirectHint: "joint-entry",
      },
    })).toBe("joint-entry");
  });

  it("returns null when redirect hint is missing", () => {
    expect(readRedirectHint({
      code: 401,
      message: "Missing auth cookie",
      data: {},
    })).toBeNull();
  });
});

describe("shouldRecoverJointSession", () => {
  it("returns true for joint-entry unauthorized payload", () => {
    expect(shouldRecoverJointSession(401, {
      code: 401,
      message: "Missing auth cookie",
      data: {
        redirectHint: "joint-entry",
      },
    })).toBe(true);
  });

  it("returns false for other statuses or hints", () => {
    expect(shouldRecoverJointSession(500, {
      data: {
        redirectHint: "joint-entry",
      },
    })).toBe(false);

    expect(shouldRecoverJointSession(401, {
      data: {
        redirectHint: "other-entry",
      },
    })).toBe(false);
  });
});
