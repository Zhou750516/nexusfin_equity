import ApprovalStepsCard from "@/components/ApprovalStepsCard";
import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatCurrency } from "@/lib/format";
import { getApprovalStatus } from "@/lib/loan-api";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath, getQueryParam } from "@/lib/route";
import type { ApprovalStatus } from "@/types/loan.types";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "wouter";

const MISSING_CONTEXT_COPY: Record<Locale, string> = {
  "zh-CN": "缺少借款申请编号，请返回首页重新发起申请。",
  "zh-TW": "缺少借款申請編號，請返回首頁重新發起申請。",
  "en-US": "Missing application ID. Please go back to the home page and start again.",
  "vi-VN": "Thiếu mã hồ sơ vay. Vui lòng quay lại trang chủ và bắt đầu lại.",
};

const PROTOCOL_PREFIX_COPY: Record<Locale, string> = {
  "zh-CN": "专享价",
  "zh-TW": "專享價",
  "en-US": "Special price",
  "vi-VN": "Giá ưu đãi",
};

const PROTOCOL_SUFFIX_COPY: Record<Locale, string> = {
  "zh-CN": "，开通即同意",
  "zh-TW": "，開通即同意",
  "en-US": ". By activating, you agree to ",
  "vi-VN": ". Khi kích hoạt, bạn đồng ý với ",
};

export default function ApprovalPendingPage() {
  const [, navigate] = useLocation();
  const loan = useLoan();
  const { locale, t } = useI18n();
  const [statusData, setStatusData] = useState<ApprovalStatus | null>(null);
  const [loadedLocale, setLoadedLocale] = useState<Locale | null>(null);
  const [loadedApplicationId, setLoadedApplicationId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [benefitDismissed, setBenefitDismissed] = useState(false);

  const applicationId = getQueryParam("applicationId") ?? loan.applicationId;
  const resultPath = applicationId ? buildPath("/approval-result", { applicationId }) : "/approval-result";
  const benefitsPath = applicationId ? buildPath("/benefits-card", { applicationId }) : "/benefits-card";

  const agreements = useMemo(() => ([
    t("approvalPending.userServiceAgreement"),
    t("approvalPending.privacyTerms"),
    t("approvalPending.debitAgreement"),
    t("approvalPending.benefitServiceAgreement"),
  ]), [t]);
  const agreementSeparator = locale.startsWith("zh") ? "" : " · ";

  useEffect(() => {
    if (applicationId && applicationId !== loan.applicationId) {
      loan.setApplicationId(applicationId);
    }
  }, [applicationId]);

  useEffect(() => {
    if (!applicationId) {
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

    void loadStatus(applicationId);
  }, [applicationId, loadedApplicationId, loadedLocale, locale]);

  useEffect(() => {
    if (!applicationId || !statusData || statusData.status === "approved" || statusData.status === "rejected") {
      return;
    }

    const intervalId = window.setInterval(() => {
      void loadStatus(applicationId, false);
    }, 10000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [applicationId, locale, statusData?.status]);

  async function loadStatus(currentApplicationId: string, showLoading = true) {
    if (showLoading) {
      setIsLoading(true);
    }
    setError(null);
    try {
      const nextStatus = await getApprovalStatus(currentApplicationId);
      setStatusData(nextStatus);
      setLoadedLocale(locale);
      setLoadedApplicationId(currentApplicationId);
      loan.setApprovalStatus(nextStatus.status);
      loan.setApplicationId(nextStatus.applicationId);

      if (nextStatus.status === "approved" || nextStatus.status === "rejected") {
        navigate(buildPath("/approval-result", { applicationId: nextStatus.applicationId }));
      }
    } catch (loadError) {
      setError(readErrorMessage(loadError));
    } finally {
      if (showLoading) {
        setIsLoading(false);
      }
    }
  }

  if (!applicationId) {
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

  if (isLoading && !statusData) {
    return (
      <MobileLayout>
        <PageLoading lines={5} />
      </MobileLayout>
    );
  }

  if (error && !statusData) {
    return (
      <MobileLayout>
        <PageError message={error} onAction={() => void loadStatus(applicationId)} />
      </MobileLayout>
    );
  }

  const benefitsCard = statusData?.benefitsCard;
  const shouldShowBenefits = Boolean(benefitsCard?.available) && !benefitDismissed;

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-36">
        <div className="bg-gradient-to-b from-[#165dff] to-[#3d7aff] px-5 pt-14 pb-28 overflow-hidden relative">
          <div className="absolute w-48 h-48 right-[-30px] top-[-20px] bg-white/10 rounded-full" />
          <div className="absolute w-36 h-36 left-[-15px] bottom-[-10px] bg-white/10 rounded-full" />
          <div className="relative flex flex-col items-center">
            <div className="w-[72px] h-[72px] bg-white/25 rounded-full flex items-center justify-center mb-5">
              <svg width="36" height="36" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M20 36.6666C29.2048 36.6666 36.6667 29.2047 36.6667 20C36.6667 10.7952 29.2048 3.33333 20 3.33333C10.7953 3.33333 3.33337 10.7952 3.33337 20C3.33337 29.2047 10.7953 36.6666 20 36.6666Z" stroke="white" strokeWidth="3.33321" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M20 10V20L26.6667 23.3333" stroke="white" strokeWidth="3.33321" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <h1 className="text-white text-[28px] font-bold mb-2">{t("approvalPending.title")}</h1>
            <p className="text-white/80 text-[14px] text-center px-4">{t("approvalPending.subtitle")}</p>
          </div>
        </div>

        <div className="px-4 -mt-16 space-y-4">
          {statusData ? <ApprovalStepsCard steps={statusData.steps} /> : null}

          {shouldShowBenefits && benefitsCard ? (
            <div className="relative bg-[#fffbf0] rounded-2xl border border-[#ffe8b8] overflow-hidden cursor-pointer" onClick={() => navigate(benefitsPath)}>
              <div className="relative p-5">
                <div className="flex justify-between items-center mb-4 gap-3">
                  <div className="flex items-center gap-2 min-w-0">
                    <div className="w-8 h-8 bg-gradient-to-br from-[#fbaf19] to-[#ff9500] rounded-[10px] flex items-center justify-center flex-shrink-0">
                      <svg width="14" height="11" viewBox="0 0 14 11" fill="none">
                        <path d="M12.375 0H1.375C0.611875 0 0.00687499 0.611875 0.00687499 1.375L0 9.625C0 10.3881 0.611875 11 1.375 11H12.375C13.1381 11 13.75 10.3881 13.75 9.625V1.375C13.75 0.611875 13.1381 0 12.375 0ZM12.375 9.625H1.375V5.5H12.375V9.625ZM12.375 2.75H1.375V1.375H12.375V2.75Z" fill="white" />
                      </svg>
                    </div>
                    <span className="text-[#333] text-[15px] font-semibold truncate">{t("approvalPending.benefitCard")}</span>
                    <div className="bg-white/80 rounded px-1.5 py-0.5 border border-[#e5e6eb] flex-shrink-0">
                      <span className="text-[#999] text-[10px]">{t("common.ad")}</span>
                    </div>
                  </div>
                  <button
                    className="flex items-center gap-1 flex-shrink-0"
                    onClick={(event) => {
                      event.stopPropagation();
                      navigate(benefitsPath);
                    }}
                  >
                    <span className="text-[#ff6b00] text-[13px] font-medium">{t("approvalPending.benefitDetail")}</span>
                    <svg width="5" height="9" viewBox="0 0 5 9" fill="none">
                      <path d="M0.624951 8.12518L4.37503 4.3751L0.624951 0.625016" stroke="#FF6B00" strokeWidth="1.16613" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </button>
                </div>

                <div className="mb-4">
                  <div className="flex items-center justify-between mb-3 gap-3">
                    <h4 className="text-[#333] text-[13px] font-semibold">{t("approvalPending.featuresTitle")}</h4>
                    <span className="text-[#ff6b00] text-[16px] font-bold">{formatCurrency(benefitsCard.price, locale)}</span>
                  </div>
                  <div className="space-y-2.5">
                    {benefitsCard.features.map((feature, index) => (
                      <div key={`${feature}-${index}`} className="flex items-center gap-2.5">
                        <div className="w-5 h-5 flex items-center justify-center flex-shrink-0">
                          <svg width="9" height="7" viewBox="0 0 9 7" fill="none">
                            <path d="M0.681843 3.95454L2.86366 6.13636L8.31821 0.68181" stroke="#165DFF" strokeWidth="1.24995" strokeLinecap="round" strokeLinejoin="round" />
                          </svg>
                        </div>
                        <span className="text-[#333] text-[13px]">{feature}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="border-t border-[#ffe8b8]/60 pt-3">
                  <p className="text-[#999] text-[11px] leading-relaxed">
                    {PROTOCOL_PREFIX_COPY[locale]}
                    {formatCurrency(benefitsCard.price, locale)}
                    {PROTOCOL_SUFFIX_COPY[locale]}
                    {agreements.map((agreement, index) => (
                      <span key={agreement}>
                        <span className="text-[#165dff]">{agreement}</span>
                        {index < agreements.length - 1 ? agreementSeparator : ""}
                      </span>
                    ))}
                  </p>
                </div>
              </div>
            </div>
          ) : null}

          {error && statusData ? <PageError message={error} onAction={() => void loadStatus(applicationId)} /> : null}
        </div>
      </div>

      {shouldShowBenefits ? (
        <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 pt-4 pb-8 border-t border-[#f2f3f5]">
          <button
            onClick={() => navigate(benefitsPath)}
            className="w-full h-14 bg-gradient-to-r from-[#165dff] to-[#4d8fff] rounded-full text-white text-[17px] font-semibold shadow-[0px_8px_24px_rgba(22,93,255,0.35)] mb-3 active:opacity-90 transition-opacity"
          >
            {t("approvalPending.activate")}
          </button>
          <button onClick={() => setBenefitDismissed(true)} className="w-full text-center text-[#86909c] text-[14px]">
            {t("approvalPending.skip")}
          </button>
        </div>
      ) : null}
    </MobileLayout>
  );
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
