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
