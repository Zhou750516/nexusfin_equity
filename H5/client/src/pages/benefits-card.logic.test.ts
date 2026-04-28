import { describe, expect, it } from "vitest";
import { canActivateBenefits, resolveDefaultBenefitsUserCard } from "./benefits-card.logic";

describe("benefits card page logic", () => {
  it("prefers the backend default card for the deduction summary", () => {
    expect(resolveDefaultBenefitsUserCard([
      { cardId: "card-001", defaultCard: false },
      { cardId: "card-002", defaultCard: true },
    ])?.cardId).toBe("card-002");
  });

  it("falls back to the first card when no backend default is marked", () => {
    expect(resolveDefaultBenefitsUserCard([
      { cardId: "card-001", defaultCard: false },
      { cardId: "card-002", defaultCard: false },
    ])?.cardId).toBe("card-001");
  });

  it("blocks activation when the application id is missing", () => {
    expect(canActivateBenefits({
      applicationId: null,
      protocolReady: true,
    })).toBe(false);
  });

  it("blocks activation when protocol readiness is not satisfied", () => {
    expect(canActivateBenefits({
      applicationId: "APP-001",
      protocolReady: false,
    })).toBe(false);
  });

  it("allows activation only when both application id and protocol readiness are present", () => {
    expect(canActivateBenefits({
      applicationId: "APP-001",
      protocolReady: true,
    })).toBe(true);
  });
});
