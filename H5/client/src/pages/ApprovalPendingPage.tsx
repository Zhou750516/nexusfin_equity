import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatCurrency } from "@/lib/format";
import { getApprovalStatus } from "@/lib/loan-api";
import { toLoanPurposeKey } from "@/lib/loan-purpose";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath, getQueryParam } from "@/lib/route";
import { shouldShowPendingBenefitsEntry } from "@/pages/approval-pending-benefits.logic";
import type { ApprovalStatus } from "@/types/loan.types";
import { ChevronRight, Clock, Sparkles } from "lucide-react";
import { useEffect, useState } from "react";
import { useLocation } from "wouter";

const MISSING_CONTEXT_COPY: Record<Locale, string> = {
  "zh-CN": "缺少借款申请编号，请返回首页重新发起申请。",
  "zh-TW": "缺少借款申請編號，請返回首頁重新發起申請。",
  "en-US": "Missing application ID. Please go back to the home page and start again.",
  "vi-VN": "Thiếu mã hồ sơ vay. Vui lòng quay lại trang chủ và bắt đầu lại.",
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
      loan.setPurpose(nextStatus.purpose ?? loan.purpose);
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
            navigate("/calculator");
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
  const purposeKey = statusData?.purpose || loan.purpose
    ? toLoanPurposeKey(statusData?.purpose ?? loan.purpose)
    : null;
  const shouldShowBenefits = shouldShowPendingBenefitsEntry({
    available: Boolean(benefitsCard?.available),
    dismissed: benefitDismissed,
  });
  const benefitsPath = applicationId
    ? buildPath("/benefits-card", { applicationId })
    : "/benefits-card";

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-[#f7f8fa] via-[#fcfdfd] to-white pb-32">
        <section
          className="px-5 pt-12 pb-12 flex flex-col items-center"
          style={{ backgroundImage: "linear-gradient(141deg, #165dff 0%, #3d8aff 100%)" }}
        >
          <div className="size-20 bg-white/20 rounded-full flex items-center justify-center mb-7">
            <Clock className="size-10 text-white" strokeWidth={2.5} />
          </div>
          <h1 className="text-white text-[28px] font-bold leading-[42px] tracking-[0.4px] text-center">
            {t("approvalPending.title")}
          </h1>
          <p className="mt-3 text-white/80 text-[15px] leading-[22.5px] tracking-tight text-center">
            {t("approvalPending.subtitle")}
          </p>
          {purposeKey ? (
            <div className="mt-5 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-4 py-2">
              <span className="text-white/70 text-xs leading-[18px] tracking-tight">
                {t("calculator.loanPurpose")}
              </span>
              <span className="text-white text-sm font-semibold leading-[21px] tracking-tight">
                {t(purposeKey)}
              </span>
            </div>
          ) : null}
        </section>

        {shouldShowBenefits ? (
          <div className="px-5 mt-6">
            <button
              type="button"
              onClick={() => navigate(benefitsPath)}
              className="w-full bg-white border border-[#ffe7d6] rounded-2xl px-5 py-4 flex items-center justify-between shadow-[0_4px_12px_rgba(251,175,25,0.08)] active:opacity-90 transition-opacity"
            >
              <div className="flex items-center gap-3">
                <div
                  className="size-10 rounded-xl flex items-center justify-center shadow-[0_4px_8px_rgba(251,175,25,0.25)]"
                  style={{ backgroundImage: "linear-gradient(135deg, #fbaf19 0%, #ff8f1f 100%)" }}
                >
                  <Sparkles className="size-5 text-white" strokeWidth={2} />
                </div>
                <div className="flex flex-col items-start gap-0.5">
                  <p className="text-[#1d2129] text-[15px] font-semibold tracking-tight">
                    {t("benefits.cardName")}
                  </p>
                  <p className="text-[#86909c] text-xs leading-[18px]">
                    {t("benefits.cardSubtitle")}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-1 shrink-0">
                <span className="text-[#fbaf19] text-sm font-medium tracking-tight">
                  {t("benefits.viewDetail")}
                </span>
                <ChevronRight className="size-4 text-[#fbaf19]" strokeWidth={2} />
              </div>
            </button>
          </div>
        ) : null}

        {error && statusData ? (
          <div className="px-5 pt-6">
            <PageError message={error} onAction={() => void loadStatus(applicationId)} />
          </div>
        ) : null}
      </div>

      {shouldShowBenefits ? (
        <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 pt-6 pb-6 flex flex-col gap-4">
          <button
            onClick={() => navigate(benefitsPath)}
            className="w-full h-14 rounded-full bg-gradient-to-r from-[#165dff] to-[#3d8aff] text-white text-[17px] font-semibold tracking-tight shadow-[0_20px_25px_rgba(22,93,255,0.4),0_8px_10px_rgba(22,93,255,0.4)] active:opacity-90 transition-opacity"
          >
            {t("approvalPending.activate")}
          </button>
          <button
            onClick={() => setBenefitDismissed(true)}
            className="w-full text-center text-[#86909c] text-[13px] font-medium tracking-tight"
          >
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
