import { describe, expect, it } from "vitest";
import { parseJointLoginParams, resolveJointEntryErrorKey, resolveJointEntryTarget } from "./joint-entry.logic";

describe("parseJointLoginParams", () => {
  it("parses supported params from search string", () => {
    expect(
      parseJointLoginParams("?token=joint-token-001&scene=push&benefitOrderNo=BEN-001&orderNo=ORD-001&productCode=PROD-001"),
    ).toEqual({
      token: "joint-token-001",
      scene: "push",
      benefitOrderNo: "BEN-001",
      orderNo: "ORD-001",
      productCode: "PROD-001",
    });
  });

  it("accepts push scene without benefit order number", () => {
    expect(
      parseJointLoginParams("?token=joint-token-push-001&scene=push"),
    ).toEqual({
      token: "joint-token-push-001",
      scene: "push",
    });
  });

  it("trims token and scene before validation", () => {
    expect(
      parseJointLoginParams("?token=%20joint-token-002%20&scene=%20refund%20&benefitOrderNo=BEN-002"),
    ).toEqual({
      token: "joint-token-002",
      scene: "refund",
      benefitOrderNo: "BEN-002",
    });
  });

  it("returns null when token is missing", () => {
    expect(parseJointLoginParams("?scene=push&benefitOrderNo=BEN-003")).toBeNull();
  });

  it("returns null when scene is unsupported", () => {
    expect(parseJointLoginParams("?token=joint-token-004&scene=unknown_scene")).toBeNull();
  });

  it("returns null when exercise scene misses benefit order number", () => {
    expect(parseJointLoginParams("?token=joint-token-005&scene=exercise")).toBeNull();
  });
});

describe("resolveJointEntryTarget", () => {
  it("maps landing page for push scene", () => {
    expect(resolveJointEntryTarget({
      loginSuccess: true,
      scene: "push",
      targetPage: "landing",
      benefitOrderNo: null,
      externalUserId: "xh-cid-001",
      localUserReady: true,
    })).toEqual("/landing");
  });

  it("maps refund page for refund scene", () => {
    expect(resolveJointEntryTarget({
      loginSuccess: true,
      scene: "refund",
      targetPage: "joint-refund-entry",
      benefitOrderNo: "BEN-20260417-002",
      externalUserId: "xh-cid-002",
      localUserReady: true,
    })).toEqual("/joint-refund-entry?scene=refund&benefitOrderNo=BEN-20260417-002");
  });

  it("maps unsupported page for unsupported target", () => {
    expect(resolveJointEntryTarget({
      loginSuccess: true,
      scene: "unknown",
      targetPage: "joint-unsupported",
      benefitOrderNo: null,
      externalUserId: null,
      localUserReady: false,
    })).toEqual("/joint-unsupported");
  });
});

describe("resolveJointEntryErrorKey", () => {
  it("maps token invalid errors to session expired copy", () => {
    expect(resolveJointEntryErrorKey("JOINT_LOGIN_TOKEN_INVALID:Joint login session expired")).toBe(
      "jointEntry.sessionExpired",
    );
  });

  it("maps missing benefit order errors to missing params copy", () => {
    expect(resolveJointEntryErrorKey("JOINT_LOGIN_BENEFIT_ORDER_REQUIRED:Benefit order number is required")).toBe(
      "jointEntry.missingParams",
    );
  });

  it("maps upstream failures to system busy copy", () => {
    expect(resolveJointEntryErrorKey("JOINT_LOGIN_UPSTREAM_FAILED:Joint login temporarily unavailable")).toBe(
      "jointEntry.systemBusy",
    );
    expect(resolveJointEntryErrorKey("JOINT_LOGIN_UPSTREAM_TIMEOUT:Joint login temporarily unavailable")).toBe(
      "jointEntry.systemBusy",
    );
  });
});
