import { describe, expect, it } from "vitest";
import { shouldShowPendingBenefitsEntry } from "./approval-pending-benefits.logic";

describe("approval pending benefits entry", () => {
  it("shows the benefits entry only when the backend marks it available and the user has not dismissed it", () => {
    expect(shouldShowPendingBenefitsEntry({
      available: true,
      dismissed: false,
    })).toBe(true);
  });

  it("hides the benefits entry after the user dismisses it", () => {
    expect(shouldShowPendingBenefitsEntry({
      available: true,
      dismissed: true,
    })).toBe(false);
  });

  it("hides the benefits entry when the backend does not provide a benefits card", () => {
    expect(shouldShowPendingBenefitsEntry({
      available: false,
      dismissed: false,
    })).toBe(false);
  });
});
