import React from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { buildApplyLoanPayload } from "./calculator.logic";
import { useCalculatorPageState } from "./useCalculatorPageState";

const navigate = vi.fn();

vi.mock("wouter", () => ({
  useLocation: () => ["/calculator", navigate] as const,
}));

vi.mock("@/i18n/I18nProvider", () => ({
  useI18n: () => ({
    locale: "zh-CN",
    t: (key: string) => key,
  }),
}));

vi.mock("@/contexts/LoanContext", () => ({
  useLoan: () => ({
    amount: 3000,
    term: 3,
    purpose: "rent",
    receivingAccountId: "acc_001",
    applicationId: null,
    setReceivingAccountId: vi.fn(),
    setAmount: vi.fn(),
    setTerm: vi.fn(),
    setPurpose: vi.fn(),
    setApplicationId: vi.fn(),
    setApprovalStatus: vi.fn(),
    setApprovalMessage: vi.fn(),
    setBenefitsCardActivated: vi.fn(),
    setBenefitOrderNo: vi.fn(),
  }),
}));

vi.mock("@/lib/loan-api", () => ({
  getCalculatorConfig: vi.fn(),
  calculateLoan: vi.fn(),
  applyLoan: vi.fn(),
}));

describe("calculator apply payload", () => {
  beforeEach(() => {
    navigate.mockReset();
  });

  it("does not expose unused setters from calculator page state", () => {
    let hookState: ReturnType<typeof useCalculatorPageState> | null = null;

    function HookProbe() {
      hookState = useCalculatorPageState();
      return null;
    }

    renderToStaticMarkup(React.createElement(HookProbe));

    expect(hookState).not.toBeNull();
    expect("setViewedProtocols" in hookState!).toBe(false);
    expect("setPurposeKey" in hookState!).toBe(false);
  });

  it("maps selected purpose key into apply payload purpose", () => {
    expect(
      buildApplyLoanPayload({
        amount: 3000,
        term: 3,
        receivingAccountId: "acc_001",
        agreedProtocols: ["user_agreement", "loan_agreement"],
        purposeKey: "calculator.loanPurpose.rent",
      }),
    ).toEqual({
      amount: 3000,
      term: 3,
      receivingAccountId: "acc_001",
      agreedProtocols: ["user_agreement", "loan_agreement"],
      purpose: "rent",
    });
  });
});
