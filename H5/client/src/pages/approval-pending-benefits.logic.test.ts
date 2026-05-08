import { describe, expect, it } from "vitest";
import { shouldShowPendingBenefitsEntry } from "./approval-pending-benefits.logic";

describe("approval pending benefits entry", () => {
  it("shows the benefits entry only when the backend marks it available and the user has not dismissed it", () => {
    expect(shouldShowPendingBenefitsEntry({
      available: true,
      dismissed: false,
      hasJointLoginToken: true,
    })).toBe(true);
  });

  it("hides the benefits entry after the user dismisses it", () => {
    expect(shouldShowPendingBenefitsEntry({
      available: true,
      dismissed: true,
      hasJointLoginToken: true,
    })).toBe(false);
  });

  it("hides the benefits entry when the backend does not provide a benefits card", () => {
    expect(shouldShowPendingBenefitsEntry({
      available: false,
      dismissed: false,
      hasJointLoginToken: true,
    })).toBe(false);
  });

  it("hides the benefits entry when the current flow has no joint-login token", () => {
    expect(shouldShowPendingBenefitsEntry({
      available: true,
      dismissed: false,
      hasJointLoginToken: false,
    })).toBe(false);
  });
});
