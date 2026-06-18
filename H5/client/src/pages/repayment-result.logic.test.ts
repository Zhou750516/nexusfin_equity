import { describe, expect, it } from "vitest";
import {
  parseRepaymentResultEntry,
  resolveRepaymentDisplayAmount,
  resolveRepaymentResultSubtitle,
  resolveRepaymentResultTime,
  shouldUseSubmittedAmountFallback,
  shouldPollRepaymentResult,
} from "./repayment-result.logic";

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

  it("prefers backend remark as the visible subtitle when present", () => {
    expect(resolveRepaymentResultSubtitle("failed", "fallback", "  还款已受理  "))
      .toBe("还款已受理");
    expect(resolveRepaymentResultSubtitle("success", "", ""))
      .toBe("");
  });

  it("parses repaymentId and positive URL amount from the result page query", () => {
    expect(parseRepaymentResultEntry("?repaymentId=xhqbapi20260618181657154625&amount=1040.260"))
      .toEqual({
        repaymentId: "xhqbapi20260618181657154625",
        amount: 1040.26,
      });
  });

  it("ignores invalid URL amounts", () => {
    expect(parseRepaymentResultEntry("?repaymentId=rp-001&amount=-1").amount).toBeNull();
    expect(parseRepaymentResultEntry("?repaymentId=rp-001&amount=0").amount).toBeNull();
    expect(parseRepaymentResultEntry("?repaymentId=rp-001&amount=abc").amount).toBeNull();
  });

  it("prefers positive backend result amount over URL and cached submitted amounts", () => {
    expect(resolveRepaymentDisplayAmount(1040.26, 999.99, 888.88)).toBe(1040.26);
  });

  it("uses URL amount when backend result amount is zero", () => {
    expect(resolveRepaymentDisplayAmount(0, 1040.26, null)).toBe(1040.26);
  });

  it("uses cached submitted amount when backend and URL amounts are unavailable", () => {
    expect(resolveRepaymentDisplayAmount(0, null, 1040.26)).toBe(1040.26);
  });

  it("falls back to backend amount when all submitted amount fallbacks are unavailable", () => {
    expect(resolveRepaymentDisplayAmount(0, null, null)).toBe(0);
  });

  it("switches from submitted amount fallback to backend amount after polling returns a positive amount", () => {
    expect(resolveRepaymentDisplayAmount(0, 1040.26, 1040.26)).toBe(1040.26);
    expect(resolveRepaymentDisplayAmount(1040.26, 999.99, 888.88)).toBe(1040.26);
  });

  it("only uses submitted amount fallback for non-positive backend result amounts", () => {
    expect(shouldUseSubmittedAmountFallback(0)).toBe(true);
    expect(shouldUseSubmittedAmountFallback(-1)).toBe(true);
    expect(shouldUseSubmittedAmountFallback(null)).toBe(true);
    expect(shouldUseSubmittedAmountFallback(1040.26)).toBe(false);
  });
});
