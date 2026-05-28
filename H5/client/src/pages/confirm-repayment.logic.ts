export type RepaymentActionStage = "send_sms" | "confirm_sms" | "submit";

export interface RepaymentActionState {
  hasBankCard: boolean;
  smsRequired: boolean;
  smsSent: boolean;
  smsVerified: boolean;
  captcha: string;
}

export interface RepaymentCardOption {
  accountId?: string;
}

export interface RepaymentUnavailableFeedback {
  titleKey: string;
  messageKey: string;
  retryKey: string;
}

export interface RepaymentSmsSectionState {
  smsRequired: boolean;
}

export interface RepaymentSubmitOutcome {
  status: "processing" | "failed";
}

export function resolveRepaymentInfoUrl(loanId: number | null | undefined): string | null {
  return loanId ? `/repayment/info/${loanId}` : null;
}

export function resolveDefaultRepaymentSubmitType(): "scheduled" {
  return "scheduled";
}

export function resolveRepaymentActionStage(state: RepaymentActionState): RepaymentActionStage {
  if (!state.smsRequired || state.smsVerified) {
    return "submit";
  }

  if (!state.smsSent) {
    return "send_sms";
  }

  return "confirm_sms";
}

export function canProceedRepaymentAction(state: RepaymentActionState): boolean {
  if (!state.hasBankCard) {
    return false;
  }

  return resolveRepaymentActionStage(state) !== "confirm_sms" || state.captcha.trim().length > 0;
}

export function shouldShowRepaymentSmsSection(state: RepaymentSmsSectionState | null | undefined): boolean {
  return Boolean(state?.smsRequired);
}

export function shouldNavigateAfterRepaymentSubmit(outcome: RepaymentSubmitOutcome): boolean {
  return outcome.status !== "failed";
}

export function resolveSelectedRepaymentCardId(
  currentSelectedCardId: string | null | undefined,
  backendSelectedCardId: string | null | undefined,
  bankCards: RepaymentCardOption[],
) {
  const cardIds = bankCards
    .map((card) => card.accountId)
    .filter((accountId): accountId is string => Boolean(accountId));

  if (currentSelectedCardId && cardIds.includes(currentSelectedCardId)) {
    return currentSelectedCardId;
  }

  if (backendSelectedCardId && cardIds.includes(backendSelectedCardId)) {
    return backendSelectedCardId;
  }

  return cardIds[0] ?? null;
}

export function resolveRepaymentUnavailableFeedback(): RepaymentUnavailableFeedback {
  return {
    titleKey: "repaymentConfirm.unavailableTitle",
    messageKey: "repaymentConfirm.unavailable",
    retryKey: "common.retry",
  };
}
