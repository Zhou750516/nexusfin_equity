import { describe, expect, it } from "vitest";
import {
  normalizeApprovalResultStatus,
  resolveApprovalResultPrimaryAction,
  shouldFetchApprovalResult,
  shouldPollApprovalResult,
} from "./approval-result.logic";

describe("approval result fetch guard", () => {
  it("skips fetching remote result when status is loan_failed even if applicationId exists", () => {
    expect(shouldFetchApprovalResult("APP-123", "loan_failed")).toBe(false);
  });

  it("fetches remote result for normal result pages with applicationId", () => {
    expect(shouldFetchApprovalResult("APP-123", "approved")).toBe(true);
    expect(shouldFetchApprovalResult("APP-123", "rejected")).toBe(true);
    expect(shouldFetchApprovalResult("APP-123", "pending")).toBe(true);
  });

  it("skips fetching when applicationId is missing", () => {
    expect(shouldFetchApprovalResult(null, "approved")).toBe(false);
  });

  it("keeps polling on the result page while the backend still reports reviewing", () => {
    expect(shouldPollApprovalResult("reviewing")).toBe(true);
    expect(shouldPollApprovalResult("approved")).toBe(false);
    expect(shouldPollApprovalResult("rejected")).toBe(false);
  });

  it("preserves reviewing as its own display state instead of collapsing it into rejected", () => {
    expect(normalizeApprovalResultStatus("reviewing", "pending")).toBe("reviewing");
    expect(normalizeApprovalResultStatus(null, "loan_failed")).toBe("loan_failed");
    expect(normalizeApprovalResultStatus("approved", "pending")).toBe("approved");
  });

  it("maps the reviewing result CTA to reload instead of sending the user home", () => {
    expect(resolveApprovalResultPrimaryAction("reviewing", false)).toBe("reload");
    expect(resolveApprovalResultPrimaryAction("approved", true)).toBe("repayment");
    expect(resolveApprovalResultPrimaryAction("rejected", false)).toBe("home");
  });
});
