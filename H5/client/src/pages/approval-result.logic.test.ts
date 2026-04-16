import { describe, expect, it } from "vitest";
import { shouldFetchApprovalResult } from "./approval-result.logic";

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
});
