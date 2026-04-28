export type RepaymentResultStatus = "processing" | "success" | "failed";

export function shouldPollRepaymentResult(status: RepaymentResultStatus | null | undefined) {
  return status === "processing";
}

export function resolveRepaymentResultTime(
  repaymentTime: string | null | undefined,
  status: RepaymentResultStatus,
) {
  if (status === "processing" && (!repaymentTime || repaymentTime.trim().length === 0)) {
    return "--";
  }

  return repaymentTime && repaymentTime.trim().length > 0 ? repaymentTime : "--";
}
