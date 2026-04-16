import { describe, expect, it } from "vitest";
import { resolveJointEntryTarget } from "./joint-entry.logic";

describe("resolveJointEntryTarget", () => {
  it("maps dispatch page for push scene", () => {
    expect(resolveJointEntryTarget({
      loginSuccess: true,
      scene: "push",
      targetPage: "joint-dispatch",
      benefitOrderNo: "BEN-20260417-001",
      localUserReady: true,
    })).toEqual("/joint-dispatch?scene=push&benefitOrderNo=BEN-20260417-001");
  });

  it("maps refund page for refund scene", () => {
    expect(resolveJointEntryTarget({
      loginSuccess: true,
      scene: "refund",
      targetPage: "joint-refund-entry",
      benefitOrderNo: "BEN-20260417-002",
      localUserReady: true,
    })).toEqual("/joint-refund-entry?scene=refund&benefitOrderNo=BEN-20260417-002");
  });
});
