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
import { CheckCircle2, ShieldCheck } from "lucide-react";
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
            navigate("/calculator");
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

  const tipBullets = [
    t("approvalResult.tip1"),
    t("approvalResult.tip2"),
    t("approvalResult.tip3"),
    t("approvalResult.tip4"),
  ];

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-[#f7f8fa] via-[#fcfdfd] to-white pb-32">
        <section
          className="relative overflow-hidden px-5 pt-12 pb-24"
          style={{
            backgroundImage: isApproved
              ? "linear-gradient(136deg, #165dff 0%, #3d8aff 100%)"
              : undefined,
          }}
        >
          {!isApproved ? (
            <div className={`absolute inset-0 bg-gradient-to-b ${headerGradientClass}`} />
          ) : null}
          <div className="absolute -right-8 -top-8 size-64 bg-white/10 rounded-full blur-3xl" />
          <div className="absolute -left-24 bottom-0 size-48 bg-white/10 rounded-full blur-3xl" />
          <div className="relative flex flex-col items-center">
            <div className="size-20 bg-white/20 rounded-full flex items-center justify-center mb-6">
              {isApproved ? (
                <CheckCircle2 className="size-10 text-white" strokeWidth={2.5} />
              ) : (
                <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
                  <circle cx="20" cy="20" r="17" stroke="white" strokeWidth="3" />
                  <path d="M14 14L26 26" stroke="white" strokeWidth="3" strokeLinecap="round" />
                  <path d="M26 14L14 26" stroke="white" strokeWidth="3" strokeLinecap="round" />
                </svg>
              )}
            </div>
            <h1 className="text-white text-[28px] font-bold leading-[42px] tracking-[0.4px] text-center">
              {displayTitle}
            </h1>
            {amountLabel ? (
              <p className="mt-4 text-white text-center leading-none">
                <span className="text-base">{t("approvalResult.amountLabel")}</span>
                <span className="text-[36px] font-bold tracking-[0.4px]">{amountLabel}</span>
                <span className="text-base">{t("approvalResult.amountUnit")}</span>
              </p>
            ) : null}
            <p className="mt-5 text-white/70 text-[13px] leading-[19.5px] tracking-tight text-center">
              {isApproved
                ? t("approvalResult.arrivalTip")
                : result?.estimatedArrivalTime && result.estimatedArrivalTime !== "--"
                ? result.estimatedArrivalTime
                : tipText}
            </p>
          </div>
        </section>

        <div className="relative z-10 px-5 -mt-20 space-y-5">
          {result?.steps.length ? <ApprovalStepsCard steps={result.steps} /> : null}

          {isApproved ? (
            <div
              className="rounded-2xl border border-[#fbaf19]/20 px-5 py-5"
              style={{
                backgroundImage:
                  "linear-gradient(155deg, #fff7e8 0%, #fff8ea 50%, #fffbf0 100%)",
              }}
            >
              <div className="flex items-center gap-2">
                <ShieldCheck className="size-5 text-[#fbaf19]" strokeWidth={2.5} />
                <h4 className="text-[#1d2129] text-[15px] font-medium tracking-tight">
                  {t("approvalResult.tipTitle")}
                </h4>
              </div>
              <ul className="mt-3 flex flex-col gap-2">
                {tipBullets.map((bullet) => (
                  <li key={bullet} className="flex gap-2 items-start">
                    <span className="text-[#fbaf19] text-[13px] leading-[19.5px]">•</span>
                    <p className="text-[#4e5969] text-[13px] leading-[19.5px] tracking-tight flex-1">
                      {bullet}
                    </p>
                  </li>
                ))}
              </ul>
            </div>
          ) : tipText ? (
            <div className="bg-[#fffbf0] rounded-2xl border border-[#ffe8b8] p-5 flex gap-3">
              <div className="size-10 bg-gradient-to-br from-[#fbaf19] to-[#ff9500] rounded-xl flex items-center justify-center shrink-0">
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
          ) : null}

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
            navigate("/calculator");
          }}
          className="w-full h-14 bg-gradient-to-r from-[#165dff] to-[#3d8aff] rounded-full text-white text-[17px] font-semibold tracking-tight shadow-[0_20px_25px_rgba(22,93,255,0.4),0_8px_10px_rgba(22,93,255,0.4)] active:opacity-90 transition-opacity"
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
