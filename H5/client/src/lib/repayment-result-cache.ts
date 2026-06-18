const SUBMITTED_REPAYMENT_AMOUNT_PREFIX = "nexusfin.repayment.submittedAmount.";

export function saveSubmittedRepaymentAmount(repaymentId: string, amount: number): void {
  if (!canUseSessionStorage() || !isValidRepaymentId(repaymentId) || !isPositiveAmount(amount)) {
    return;
  }
  window.sessionStorage.setItem(buildSubmittedAmountKey(repaymentId), String(amount));
}

export function readSubmittedRepaymentAmount(repaymentId: string): number | null {
  if (!canUseSessionStorage() || !isValidRepaymentId(repaymentId)) {
    return null;
  }
  const value = window.sessionStorage.getItem(buildSubmittedAmountKey(repaymentId));
  if (!value) {
    return null;
  }
  const parsed = Number(value);
  return isPositiveAmount(parsed) ? parsed : null;
}

export function clearSubmittedRepaymentAmount(repaymentId: string): void {
  if (!canUseSessionStorage() || !isValidRepaymentId(repaymentId)) {
    return;
  }
  window.sessionStorage.removeItem(buildSubmittedAmountKey(repaymentId));
}

function buildSubmittedAmountKey(repaymentId: string): string {
  return `${SUBMITTED_REPAYMENT_AMOUNT_PREFIX}${repaymentId}`;
}

function canUseSessionStorage(): boolean {
  return typeof window !== "undefined" && Boolean(window.sessionStorage);
}

function isValidRepaymentId(repaymentId: string): boolean {
  return repaymentId.trim().length > 0;
}

function isPositiveAmount(amount: number): boolean {
  return Number.isFinite(amount) && amount > 0;
}
