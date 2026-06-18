import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  readSubmittedRepaymentAmount,
  saveSubmittedRepaymentAmount,
} from "./repayment-result-cache";

function createStorage() {
  const store = new Map<string, string>();
  return {
    getItem(key: string) {
      return store.get(key) ?? null;
    },
    setItem(key: string, value: string) {
      store.set(key, value);
    },
    removeItem(key: string) {
      store.delete(key);
    },
    clear() {
      store.clear();
    },
  };
}

describe("repayment result submitted amount cache", () => {
  const sessionStorage = createStorage();

  beforeEach(() => {
    vi.stubGlobal("window", { sessionStorage });
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("stores and reads submitted repayment amount by repaymentId", () => {
    saveSubmittedRepaymentAmount("xhqbapi20260618181657154625", 1040.26);

    expect(readSubmittedRepaymentAmount("xhqbapi20260618181657154625")).toBe(1040.26);
  });

  it("does not store non-positive submitted amounts", () => {
    saveSubmittedRepaymentAmount("xhqbapi20260618181657154625", 0);

    expect(readSubmittedRepaymentAmount("xhqbapi20260618181657154625")).toBeNull();
  });

  it("does not expose submitted amounts across repaymentIds", () => {
    saveSubmittedRepaymentAmount("rp-001", 1040.26);

    expect(readSubmittedRepaymentAmount("rp-002")).toBeNull();
  });
});
