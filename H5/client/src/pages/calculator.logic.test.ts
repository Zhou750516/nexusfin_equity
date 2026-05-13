import React from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CalculatorBindCardDialogContent } from "@/components/calculator/CalculatorBindCardDialog";
import CalculatorHero from "@/components/calculator/CalculatorHero";
import CalculatorRepaymentSection from "@/components/calculator/CalculatorRepaymentSection";
import {
  buildApplyLoanPayload,
  resolveCalculatorSubmitDisabled,
  resolvePlatformBenefitOrderNo,
} from "./calculator.logic";
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

  it("maps selected purpose key and required backend fields into apply payload", () => {
    expect(
      buildApplyLoanPayload({
        amount: 3000,
        orderAmount: 300,
        term: 3,
        receivingAccountId: "acc_001",
        agreedProtocols: ["user_agreement", "loan_agreement"],
        purposeKey: "calculator.loanPurpose.rent",
        platformBenefitOrderNo: "PBO-001",
      }),
    ).toEqual({
      amount: 3000,
      orderAmount: 300,
      term: 3,
      receivingAccountId: "acc_001",
      agreedProtocols: ["user_agreement", "loan_agreement"],
      purpose: "rent",
      platformBenefitOrderNo: "PBO-001",
    });
  });

  it("allows apply payload without platform benefit order number", () => {
    expect(
      buildApplyLoanPayload({
        amount: 3000,
        orderAmount: 300,
        term: 3,
        receivingAccountId: "acc_001",
        agreedProtocols: ["user_agreement", "loan_agreement"],
        purposeKey: "calculator.loanPurpose.rent",
        platformBenefitOrderNo: null,
      }),
    ).toEqual({
      amount: 3000,
      orderAmount: 300,
      term: 3,
      receivingAccountId: "acc_001",
      agreedProtocols: ["user_agreement", "loan_agreement"],
      purpose: "rent",
    });
  });

  it("resolves platform benefit order number from joint-login params", () => {
    expect(resolvePlatformBenefitOrderNo({
      token: "joint-token-001",
      scene: "push",
      orderNo: "PBO-ORDER-001",
      benefitOrderNo: "BEN-LOCAL-001",
    })).toBe("PBO-ORDER-001");

    expect(resolvePlatformBenefitOrderNo({
      token: "joint-token-002",
      scene: "exercise",
      benefitOrderNo: "BEN-FALLBACK-001",
    })).toBe("BEN-FALLBACK-001");

    expect(resolvePlatformBenefitOrderNo(null)).toBeNull();
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

  it("does not disable submit just because platform benefit order number is missing", () => {
    expect(resolvePlatformBenefitOrderNo(null)).toBeNull();
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

  it("renders first and expanded repayment dates as full YYYY-MM-DD values", () => {
    const result: CalculateResult = {
      totalFee: 120.75,
      annualRate: "24.0%",
      repaymentPlan: [
        { period: 1, date: "2026-06-13", principal: 992.95, interest: 21.25, total: 1040.25 },
        { period: 2, date: "2026-07-13", principal: 999.98, interest: 14.22, total: 1040.25 },
      ],
    };

    const markup = renderToStaticMarkup(
      React.createElement(CalculatorRepaymentSection, {
        isCalculating: false,
        calculateResult: result,
        firstRepayment: result.repaymentPlan[0],
        expanded: true,
        onToggleExpanded: vi.fn(),
        onRetry: vi.fn(),
        locale: "zh-CN",
        error: null,
        t: (key: string, params?: Record<string, string | number>) =>
          key === "calculator.periodLabel" ? `第${params?.period}期` : key,
      }),
    );

    expect(markup).toContain("2026-06-13");
    expect(markup).toContain("2026-07-13");
    expect(markup).not.toContain("6/13");
    expect(markup).not.toContain("7/13");
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
    orderAmount: 300,
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
