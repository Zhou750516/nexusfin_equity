import { toLoanPurpose } from "@/lib/loan-purpose";
import type { ApplyParams, CalculateResult, CalculatorConfig } from "@/types/loan.types";

export function buildApplyLoanPayload(input: {
  amount: number;
  term: number;
  receivingAccountId: string;
  agreedProtocols: string[];
  purposeKey: string;
}): ApplyParams {
  return {
    amount: input.amount,
    term: input.term,
    receivingAccountId: input.receivingAccountId,
    agreedProtocols: input.agreedProtocols,
    purpose: toLoanPurpose(input.purposeKey),
  };
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
