import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatBankCard, formatCurrency, formatDateTime } from "@/lib/format";
import { getRepaymentResult } from "@/lib/loan-api";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { getQueryParam } from "@/lib/route";
import type { RepaymentResult } from "@/types/loan.types";
import { Calendar, CheckCircle2, CreditCard, TrendingDown } from "lucide-react";
import { useEffect, useState } from "react";
import { useLocation } from "wouter";

const MISSING_CONTEXT_COPY: Record<Locale, string> = {
  "zh-CN": "缺少还款流水号，请返回首页重新进入。",
  "zh-TW": "缺少還款流水號，請返回首頁重新進入。",
  "en-US": "Missing repayment ID. Please return to the home page and re-enter the page.",
  "vi-VN": "Thiếu mã giao dịch hoàn trả. Vui lòng quay lại trang chủ và vào lại trang này.",
};

export default function RepaymentSuccessPage() {
  const [, navigate] = useLocation();
  const loan = useLoan();
  const { locale, t } = useI18n();
  const [result, setResult] = useState<RepaymentResult | null>(null);
  const [loadedLocale, setLoadedLocale] = useState<Locale | null>(null);
  const [loadedRepaymentId, setLoadedRepaymentId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const repaymentId = getQueryParam("repaymentId") ?? loan.repaymentId;

  useEffect(() => {
    if (repaymentId && repaymentId !== loan.repaymentId) {
      loan.setRepaymentId(repaymentId);
    }
  }, [repaymentId]);

  useEffect(() => {
    if (!repaymentId) {
      setIsLoading(false);
      return;
    }

    if (!shouldRequestLocalizedData({
      locale,
      loadedLocale,
      requestKey: repaymentId,
      loadedRequestKey: loadedRepaymentId,
    })) {
      return;
    }

    void loadResult(repaymentId);
  }, [loadedLocale, loadedRepaymentId, locale, repaymentId]);

  async function loadResult(currentRepaymentId: string) {
    setIsLoading(true);
    setError(null);
    try {
      const nextResult = await getRepaymentResult(currentRepaymentId);
      setResult(nextResult);
      setLoadedLocale(locale);
      setLoadedRepaymentId(currentRepaymentId);
    } catch (loadError) {
      setError(readErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  }

  if (!repaymentId) {
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

  if (isLoading && !result) {
    return (
      <MobileLayout>
        <PageLoading lines={5} />
      </MobileLayout>
    );
  }

  if (error && !result) {
    return (
      <MobileLayout>
        <PageError message={error} onAction={() => void loadResult(repaymentId)} />
      </MobileLayout>
    );
  }

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-[#f7f8fa] via-[#fcfdfd] to-white pb-32">
        <section
          className="relative overflow-hidden px-5 pt-12 pb-24"
          style={{ backgroundImage: "linear-gradient(138deg, #165dff 0%, #3d8aff 100%)" }}
        >
          <div className="absolute -right-8 -top-8 size-64 bg-white/10 rounded-full blur-3xl" />
          <div className="absolute -left-24 bottom-0 size-48 bg-white/10 rounded-full blur-3xl" />
          <div className="relative flex flex-col items-center">
            <div className="size-20 bg-white/20 rounded-full flex items-center justify-center mb-6">
              <CheckCircle2 className="size-10 text-white" strokeWidth={2.5} />
            </div>
            <h1 className="text-white text-[28px] font-bold leading-[42px] tracking-[0.4px] text-center">
              {t("repaymentSuccess.title")}
            </h1>
            {result ? (
              <p className="mt-4 text-white text-center leading-none">
                <span className="text-base">{t("repaymentSuccess.amountLabel")}</span>
                <span className="text-[36px] font-bold tracking-[0.4px]">
                  {formatCurrency(result.amount, locale)}
                </span>
                <span className="text-base">元</span>
              </p>
            ) : null}
          </div>
        </section>

        <div className="relative z-10 px-5 -mt-20 space-y-4">
          {result ? (
            <>
              <section className="bg-white rounded-2xl shadow-[0_10px_15px_rgba(0,0,0,0.1),0_4px_6px_rgba(0,0,0,0.1)] px-6 pt-6 pb-6">
                <h3 className="text-[#1d2129] text-[17px] font-semibold leading-[25.5px] tracking-tight pb-4 border-b border-[#e5e6eb]">
                  {t("repaymentSuccess.detailTitle")}
                </h3>
                <div className="mt-4 flex flex-col gap-4">
                  <DetailRow
                    icon={<Calendar className="size-5 text-[#86909c]" strokeWidth={2} />}
                    label={t("repaymentSuccess.time")}
                    value={formatDateTime(result.repaymentTime, locale)}
                  />
                  <DetailRow
                    icon={<CreditCard className="size-5 text-[#86909c]" strokeWidth={2} />}
                    label={t("repaymentSuccess.card")}
                    value={formatBankCard(result.bankCard.bankName, result.bankCard.lastFour, locale)}
                  />
                  <DetailRow
                    icon={<TrendingDown className="size-5 text-[#00b42a]" strokeWidth={2} />}
                    label={t("repaymentSuccess.savedInterest")}
                    value={formatCurrency(result.interestSaved, locale)}
                    valueClassName="text-[#00b42a]"
                  />
                </div>
              </section>

              <section className="bg-white rounded-2xl shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)] px-5 pt-5 pb-5">
                <div className="flex items-center gap-2">
                  <span className="w-1 h-4 bg-[#165dff] rounded-[4px]" />
                  <h3 className="text-[#1d2129] text-base font-semibold leading-6 tracking-tight">
                    {t("repaymentSuccess.tipTitle")}
                  </h3>
                </div>
                <ul className="mt-3 flex flex-col gap-2">
                  {result.tips.map((tip) => (
                    <li key={tip} className="flex gap-2 items-start">
                      <span className="text-[#165dff] text-sm leading-[22.75px]">•</span>
                      <p className="flex-1 text-[#4e5969] text-sm leading-[22.75px] tracking-tight">
                        {tip}
                      </p>
                    </li>
                  ))}
                </ul>
              </section>
            </>
          ) : null}

          {error && result ? <PageError message={error} onAction={() => void loadResult(repaymentId)} /> : null}
        </div>
      </div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 py-4 border-t border-[#f2f3f5]">
        <button
          onClick={() => {
            loan.reset();
            navigate("/calculator");
          }}
          className="w-full h-14 bg-gradient-to-r from-[#165dff] to-[#3d8aff] rounded-full text-white text-[17px] font-semibold tracking-tight shadow-[0_20px_25px_rgba(22,93,255,0.4),0_8px_10px_rgba(22,93,255,0.4)] active:opacity-90 transition-opacity"
        >
          {t("repaymentSuccess.backHome")}
        </button>
      </div>
    </MobileLayout>
  );
}

function DetailRow({
  icon,
  label,
  value,
  valueClassName,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  valueClassName?: string;
}) {
  return (
    <div className="flex items-start gap-3">
      <div className="size-10 rounded-full bg-[#f2f3f5] flex items-center justify-center shrink-0">
        {icon}
      </div>
      <div className="flex-1 min-w-0 flex flex-col gap-0.5 pt-1">
        <p className="text-[#86909c] text-[13px] leading-[19.5px] tracking-tight">{label}</p>
        <p
          className={`text-[#1d2129] text-[15px] font-medium leading-[22.5px] tracking-tight ${
            valueClassName ?? ""
          }`}
        >
          {value}
        </p>
      </div>
    </div>
  );
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
