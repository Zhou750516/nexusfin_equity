export type ApprovalResultFetchStatus =
  | "idle"
  | "pending"
  | "reviewing"
  | "approved"
  | "rejected"
  | "loan_failed";

export function shouldFetchApprovalResult(
  applicationId: string | null | undefined,
  status: ApprovalResultFetchStatus,
) {
  if (!applicationId) {
    return false;
  }

  return status !== "loan_failed";
}

export function shouldPollApprovalResult(status: ApprovalResultFetchStatus | null | undefined) {
  return status === "reviewing";
}

export function normalizeApprovalResultStatus(
  remoteStatus: ApprovalResultFetchStatus | null | undefined,
  fallbackStatus: ApprovalResultFetchStatus,
) {
  return remoteStatus ?? fallbackStatus;
}

export function resolveApprovalResultPrimaryAction(
  status: ApprovalResultFetchStatus,
  hasLoanId: boolean,
) {
  if (status === "approved" && hasLoanId) {
    return "repayment";
  }

  if (status === "reviewing") {
    return "reload";
  }

  return "home";
}
