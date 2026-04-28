import { describe, expect, it } from "vitest";
import { resolveRepaymentResultTime, shouldPollRepaymentResult } from "./repayment-result.logic";

describe("repayment result page logic", () => {
  it("keeps polling while the repayment result is still processing", () => {
    expect(shouldPollRepaymentResult("processing")).toBe(true);
    expect(shouldPollRepaymentResult("success")).toBe(false);
    expect(shouldPollRepaymentResult("failed")).toBe(false);
  });

  it("shows placeholder time while processing has not produced a completion timestamp", () => {
    expect(resolveRepaymentResultTime("", "processing")).toBe("--");
    expect(resolveRepaymentResultTime(undefined, "processing")).toBe("--");
  });

  it("preserves the backend completion timestamp for terminal states", () => {
    expect(resolveRepaymentResultTime("2026-04-28T08:30:00+08:00", "success"))
      .toBe("2026-04-28T08:30:00+08:00");
    expect(resolveRepaymentResultTime("2026-04-28T08:30:00+08:00", "failed"))
      .toBe("2026-04-28T08:30:00+08:00");
  });
});
