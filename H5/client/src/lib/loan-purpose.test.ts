import { describe, expect, it } from "vitest";
import { isLoanPurpose, toLoanPurpose, toLoanPurposeKey } from "./loan-purpose";

describe("loan purpose helpers", () => {
  it("recognizes supported loan purpose values", () => {
    expect(isLoanPurpose("rent")).toBe(true);
    expect(isLoanPurpose("travel")).toBe(true);
    expect(isLoanPurpose("unknown")).toBe(false);
  });

  it("normalizes purpose keys into backend enum values", () => {
    expect(toLoanPurpose("calculator.loanPurpose.education")).toBe("education");
    expect(toLoanPurpose("calculator.loanPurpose.invalid")).toBe("shopping");
  });

  it("builds translation keys from stored purpose values", () => {
    expect(toLoanPurposeKey("rent")).toBe("calculator.loanPurpose.rent");
    expect(toLoanPurposeKey(null)).toBe("calculator.loanPurpose.shopping");
  });
});
