import React from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CalculatorBindCardDialogContent } from "@/components/calculator/CalculatorBindCardDialog";
import CalculatorHero from "@/components/calculator/CalculatorHero";
import { buildApplyLoanPayload, resolveCalculatorSubmitDisabled } from "./calculator.logic";
import { useCalculatorPageState } from "./useCalculatorPageState";
import type { CalculateResult, CalculatorConfig } from "@/types/loan.types";

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
    expect(hookState!.isAmountEditDisabled).toBe(true);
    hookState!.openAmountDrawer();
    expect(hookState!.drawerOpen).toBe(false);
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

  it("renders edit amount entry as disabled while keeping it visible", () => {
    const markup = renderToStaticMarkup(
      React.createElement(CalculatorHero, {
        amount: 3000,
        locale: "zh-CN",
        amountRangeLabel: "¥100 - ¥5000",
        onEdit: vi.fn(),
        isAmountEditDisabled: true,
        t: (key: string) => key,
      }),
    );

    expect(markup).toContain("calculator.editAmount");
    expect(markup).toContain("disabled");
    expect(markup).toContain("cursor-not-allowed");
  });

  it("disables submit when calculator config requires card binding", () => {
    expect(
      resolveCalculatorSubmitDisabled({
        config: calculatorConfig({ bindCardRequired: true, receivingAccount: null }),
        calculateResult: calculateResult(),
        isSubmitting: false,
        isCalculating: false,
      }),
    ).toBe(true);
  });

  it("keeps submit available when calculator config has a receiving account and calculation result", () => {
    expect(
      resolveCalculatorSubmitDisabled({
        config: calculatorConfig(),
        calculateResult: calculateResult(),
        isSubmitting: false,
        isCalculating: false,
      }),
    ).toBe(false);
  });

  it("renders bind-card prompt copy without turning the page into a fatal error", () => {
    const markup = renderToStaticMarkup(
      React.createElement(CalculatorBindCardDialogContent, {
        message: "请到科技平台绑卡后重试",
        onAck: vi.fn(),
        onBack: vi.fn(),
        t: (key: string) => key,
      }),
    );

    expect(markup).toContain("请到科技平台绑卡后重试");
    expect(markup).toContain("calculator.bindCardAck");
    expect(markup).toContain("calculator.bindCardBack");
  });
});

function calculatorConfig(overrides: Partial<CalculatorConfig> = {}): CalculatorConfig {
  return {
    amountRange: {
      min: 100,
      max: 5000,
      step: 100,
      default: 3000,
    },
    termOptions: [{ label: "3期", value: 3 }],
    annualRate: 0.18,
    lender: "XX商业银行",
    receivingAccount: {
      bankName: "测试银行",
      lastFour: "1234",
      accountId: "acc_001",
    },
    bindCardRequired: false,
    bindCardMessage: null,
    ...overrides,
  };
}

function calculateResult(): CalculateResult {
  return {
    totalFee: 120.75,
    annualRate: "24.0%",
    repaymentPlan: [],
  };
}
