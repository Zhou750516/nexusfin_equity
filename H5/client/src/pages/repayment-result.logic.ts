export type RepaymentResultStatus = "processing" | "success" | "failed";

export interface RepaymentResultEntry {
  repaymentId: string | null;
  amount: number | null;
}

export function shouldPollRepaymentResult(status: RepaymentResultStatus | null | undefined) {
  return status === "processing";
}

export function parseRepaymentResultEntry(search: string): RepaymentResultEntry {
  const params = new URLSearchParams(search.startsWith("?") ? search.slice(1) : search);
  return {
    repaymentId: normalizeText(params.get("repaymentId")),
    amount: parsePositiveAmount(params.get("amount")),
  };
}

export function shouldUseSubmittedAmountFallback(resultAmount: number | null | undefined): boolean {
  return typeof resultAmount !== "number" || !Number.isFinite(resultAmount) || resultAmount <= 0;
}

export function resolveRepaymentDisplayAmount(
  resultAmount: number | null | undefined,
  urlAmount: number | null | undefined,
  cachedAmount: number | null | undefined,
): number {
  if (isPositiveAmount(resultAmount)) {
    return resultAmount;
  }

  if (isPositiveAmount(urlAmount)) {
    return urlAmount;
  }

  if (isPositiveAmount(cachedAmount)) {
    return cachedAmount;
  }

  return typeof resultAmount === "number" && Number.isFinite(resultAmount) ? resultAmount : 0;
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

export function resolveRepaymentResultSubtitle(
  status: RepaymentResultStatus,
  fallbackSubtitle: string,
  remark: string | null | undefined,
) {
  const normalizedRemark = remark?.trim();
  if (normalizedRemark) {
    return normalizedRemark;
  }

  return fallbackSubtitle;
}

function normalizeText(value: string | null): string | null {
  if (!value) {
    return null;
  }
  const normalized = value.trim();
  return normalized ? normalized : null;
}

function parsePositiveAmount(value: string | null): number | null {
  const normalized = normalizeText(value);
  if (!normalized) {
    return null;
  }
  const parsed = Number(normalized);
  return isPositiveAmount(parsed) ? parsed : null;
}

function isPositiveAmount(value: number | null | undefined): value is number {
  return typeof value === "number" && Number.isFinite(value) && value > 0;
}
