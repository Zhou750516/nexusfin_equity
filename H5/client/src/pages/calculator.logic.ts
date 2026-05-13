import { toLoanPurpose } from "@/lib/loan-purpose";
import type { ApplyParams, CalculateResult, CalculatorConfig, JointLoginParams } from "@/types/loan.types";

export function buildApplyLoanPayload(input: {
  amount: number;
  orderAmount: number;
  term: number;
  receivingAccountId: string;
  agreedProtocols: string[];
  purposeKey: string;
  platformBenefitOrderNo: string;
}): ApplyParams {
  return {
    amount: input.amount,
    orderAmount: input.orderAmount,
    term: input.term,
    receivingAccountId: input.receivingAccountId,
    agreedProtocols: input.agreedProtocols,
    purpose: toLoanPurpose(input.purposeKey),
    platformBenefitOrderNo: input.platformBenefitOrderNo,
  };
}

export function resolvePlatformBenefitOrderNo(params: JointLoginParams | null): string | null {
  return params?.orderNo ?? params?.benefitOrderNo ?? null;
}

export function resolveCalculatorSubmitDisabled(input: {
  config: CalculatorConfig | null;
  calculateResult: CalculateResult | null;
  isSubmitting: boolean;
  isCalculating: boolean;
}): boolean {
  return (
    !input.config
    || input.config.bindCardRequired === true
    || !input.config.receivingAccount?.accountId
    || !input.calculateResult
    || input.isSubmitting
    || input.isCalculating
  );
}
