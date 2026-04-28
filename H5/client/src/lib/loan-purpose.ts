import type { LoanPurpose } from "@/types/loan.types";

export const DEFAULT_LOAN_PURPOSE: LoanPurpose = "shopping";

export function isLoanPurpose(value: unknown): value is LoanPurpose {
  return value === "shopping"
    || value === "rent"
    || value === "education"
    || value === "travel";
}

export function toLoanPurpose(purposeKey: string): LoanPurpose {
  const rawPurpose = purposeKey.replace("calculator.loanPurpose.", "");
  return isLoanPurpose(rawPurpose) ? rawPurpose : DEFAULT_LOAN_PURPOSE;
}

export function toLoanPurposeKey(purpose?: LoanPurpose | null): `calculator.loanPurpose.${LoanPurpose}` {
  const normalizedPurpose = purpose ?? DEFAULT_LOAN_PURPOSE;
  return `calculator.loanPurpose.${normalizedPurpose}`;
}
