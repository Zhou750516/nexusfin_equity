import ApprovalStepsCard from "@/components/ApprovalStepsCard";
import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatCurrency } from "@/lib/format";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { getApprovalResult } from "@/lib/loan-api";
import { buildPath, getQueryParam } from "@/lib/route";
import { shouldFetchApprovalResult } from "@/pages/approval-result.logic";
import type { ApprovalResult } from "@/types/loan.types";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "wouter";

const MISSING_CONTEXT_COPY: Record<Locale, string> = {
  "zh-CN": "缺少借款申请信息，请返回首页重新开始。",
  "zh-TW": "缺少借款申請資訊，請返回首頁重新開始。",
  "en-US": "Missing loan application information. Please go back to the home page and start again.",
  "vi-VN": "Thiếu thông tin hồ sơ vay. Vui lòng quay lại trang chủ và bắt đầu lại.",
};

const REJECTED_TITLE_COPY: Record<Locale, string> = {
  "zh-CN": "审批未通过",
  "zh-TW": "審批未通過",
  "en-US": "Application Rejected",
  "vi-VN": "Đơn vay chưa được duyệt",
};

const LOAN_FAILED_TITLE_COPY: Record<Locale, string> = {
  "zh-CN": "借款申请失败",
  "zh-TW": "借款申請失敗",
  "en-US": "Loan Application Failed",
  "vi-VN": "Gửi yêu cầu vay thất bại",
};

export default function ApprovalResultPage() {
  const [, navigate] = useLocation();
  const loan = useLoan();
  const { locale, t } = useI18n();
  const [result, setResult] = useState<ApprovalResult | null>(null);
  const [loadedLocale, setLoadedLocale] = useState<Locale | null>(null);
  const [loadedApplicationId, setLoadedApplicationId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const applicationId = getQueryParam("applicationId") ?? loan.applicationId;
  const isLoanFailedFallback = loan.approvalStatus === "loan_failed";

  useEffect(() => {
    if (applicationId && applicationId !== loan.applicationId) {
      loan.setApplicationId(applicationId);
    }
  }, [applicationId]);

  useEffect(() => {
    if (!shouldFetchApprovalResult(applicationId, loan.approvalStatus)) {
      setIsLoading(false);
      return;
    }

    if (!shouldRequestLocalizedData({
      locale,
      loadedLocale,
      requestKey: applicationId,
      loadedRequestKey: loadedApplicationId,
    })) {
      return;
    }

    if (applicationId) {
      void loadResult(applicationId);
    }
  }, [applicationId, loadedApplicationId, loadedLocale, loan.approvalStatus, locale]);

  async function loadResult(currentApplicationId: string) {
    setIsLoading(true);
    setError(null);
    try {
      const nextResult = await getApprovalResult(currentApplicationId);
      setResult(nextResult);
      setLoadedLocale(locale);
      setLoadedApplicationId(currentApplicationId);
      loan.setApprovalStatus(nextResult.status);
      loan.setBenefitsCardActivated(nextResult.benefitsCardActivated);
      loan.setLoanId(nextResult.loanId);
      loan.setApprovalMessage(nextResult.tip);
    } catch (loadError) {
      setError(readErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  }

  const displayStatus = result?.status ?? (isLoanFailedFallback ? "loan_failed" : loan.approvalStatus);
  const displayTitle = useMemo(() => {
    if (displayStatus === "approved") {
      return t("approvalResult.title");
    }
    if (displayStatus === "loan_failed") {
      return LOAN_FAILED_TITLE_COPY[locale];
    }
    return REJECTED_TITLE_COPY[locale];
  }, [displayStatus, locale, t]);

  const headerGradientClass = displayStatus === "approved"
    ? "from-[#165dff] to-[#3d7aff]"
    : "from-[#ff9500] to-[#ff6b00]";

  if (!applicationId && !isLoanFailedFallback) {
    return (
      <MobileLayout>
        <PageError
          message={MISSING_CONTEXT_COPY[locale]}
          onAction={() => {
            loan.reset();
            navigate("/");
          }}
          actionLabel={t("repaymentSuccess.backHome")}
        />
      </MobileLayout>
    );
  }

  if (isLoading && !result && !isLoanFailedFallback) {
    return (
      <MobileLayout>
        <PageLoading lines={5} />
      </MobileLayout>
    );
  }

  if (error && !result && !isLoanFailedFallback) {
    return (
      <MobileLayout>
        <PageError message={error} onAction={() => applicationId ? void loadResult(applicationId) : undefined} />
      </MobileLayout>
    );
  }

  const amountLabel = result && result.status === "approved"
    ? formatCurrency(result.approvedAmount, locale)
    : null;

  const tipText = result?.tip ?? loan.approvalMessage ?? "";
  const loanId = result?.loanId ?? loan.loanId;
  const isApproved = result?.status === "approved";

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-24">
        <div className={`bg-gradient-to-b ${headerGradientClass} px-5 pt-14 pb-28 overflow-hidden relative`}>
          <div className="absolute w-48 h-48 right-[-30px] top-[-20px] bg-white/10 rounded-full" />
          <div className="absolute w-36 h-36 left-[-15px] bottom-[-10px] bg-white/10 rounded-full" />
          <div className="relative flex flex-col items-center">
            <div className="w-[72px] h-[72px] bg-white/25 rounded-full flex items-center justify-center mb-5">
              {isApproved ? (
                <svg width="36" height="36" viewBox="0 0 40 40" fill="none">
                  <circle cx="20" cy="20" r="17" stroke="white" strokeWidth="3" />
                  <path d="M12 20L17 25L28 14" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              ) : (
                <svg width="36" height="36" viewBox="0 0 40 40" fill="none">
                  <circle cx="20" cy="20" r="17" stroke="white" strokeWidth="3" />
                  <path d="M14 14L26 26" stroke="white" strokeWidth="3" strokeLinecap="round" />
                  <path d="M26 14L14 26" stroke="white" strokeWidth="3" strokeLinecap="round" />
                </svg>
              )}
            </div>
            <h1 className="text-white text-[28px] font-bold mb-3">{displayTitle}</h1>
            {amountLabel ? (
              <div className="flex flex-col items-center gap-1 mb-2">
                <span className="text-white/80 text-base">{t("approvalResult.amountLabel")}</span>
                <span className="text-white text-[36px] font-bold">{amountLabel}</span>
              </div>
            ) : null}
            <p className="text-white/80 text-[13px] text-center px-4">{result?.estimatedArrivalTime && result.estimatedArrivalTime !== "--" ? result.estimatedArrivalTime : tipText}</p>
          </div>
        </div>

        <div className="px-4 -mt-16 space-y-4">
          {result?.steps.length ? <ApprovalStepsCard steps={result.steps} /> : null}

          <div className="bg-[#fffbf0] rounded-2xl border border-[#ffe8b8] p-5">
            <div className="flex gap-3">
              <div className="w-10 h-10 bg-gradient-to-br from-[#fbaf19] to-[#ff9500] rounded-xl flex items-center justify-center flex-shrink-0">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="white" strokeWidth="2" />
                  <path d="M12 8V12" stroke="white" strokeWidth="2" strokeLinecap="round" />
                  <circle cx="12" cy="16" r="1" fill="white" />
                </svg>
              </div>
              <div>
                <h4 className="text-[#8b4513] text-[13px] font-semibold mb-1">{t("approvalResult.tipTitle")}</h4>
                <p className="text-[#8b4513] text-[13px] leading-relaxed">{tipText}</p>
              </div>
            </div>
          </div>

          {error && result ? <PageError message={error} onAction={() => applicationId ? void loadResult(applicationId) : undefined} /> : null}
        </div>
      </div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 py-4 border-t border-[#f2f3f5]">
        <button
          onClick={() => {
            if (isApproved && loanId) {
              navigate(buildPath("/confirm-repayment", { loanId }));
              return;
            }
            loan.reset();
            navigate("/");
          }}
          className="w-full h-14 bg-gradient-to-r from-[#165dff] to-[#4d8fff] rounded-full text-white text-[17px] font-semibold shadow-[0px_8px_24px_rgba(22,93,255,0.35)] active:opacity-90 transition-opacity"
        >
          {isApproved && loanId ? t("approvalResult.cta") : t("repaymentSuccess.backHome")}
        </button>
      </div>
    </MobileLayout>
  );
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
