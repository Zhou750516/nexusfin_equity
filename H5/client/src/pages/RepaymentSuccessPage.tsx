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
            navigate("/");
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
      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-24">
        <div className="bg-gradient-to-b from-[#165dff] to-[#3d7aff] px-5 pt-14 pb-28 overflow-hidden relative">
          <div className="absolute w-48 h-48 right-[-30px] top-[-20px] bg-white/10 rounded-full" />
          <div className="absolute w-36 h-36 left-[-15px] bottom-[-10px] bg-white/10 rounded-full" />
          <div className="relative flex flex-col items-center">
            <div className="w-[72px] h-[72px] bg-white/25 rounded-full flex items-center justify-center mb-5">
              <svg width="36" height="36" viewBox="0 0 40 40" fill="none">
                <circle cx="20" cy="20" r="17" stroke="white" strokeWidth="3" />
                <path d="M12 20L17 25L28 14" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <h1 className="text-white text-[28px] font-bold mb-3">{t("repaymentSuccess.title")}</h1>
            {result ? (
              <div className="flex flex-col items-center gap-1">
                <span className="text-white/80 text-base">{t("repaymentSuccess.amountLabel")}</span>
                <span className="text-white text-[36px] font-bold">{formatCurrency(result.amount, locale)}</span>
              </div>
            ) : null}
          </div>
        </div>

        <div className="px-4 -mt-16 space-y-4">
          {result ? (
            <>
              <div className="bg-white rounded-2xl shadow-[0px_4px_20px_rgba(0,0,0,0.08)] px-5 pt-5 pb-5">
                <h3 className="text-[#1d2129] text-[15px] font-bold mb-4">{t("repaymentSuccess.detailTitle")}</h3>
                <div className="border-t border-[#f2f3f5] pt-4 space-y-5">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-[#f0f4ff] rounded-full flex items-center justify-center flex-shrink-0">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                        <rect x="3" y="4" width="18" height="18" rx="2" stroke="#165DFF" strokeWidth="1.5" />
                        <path d="M3 9H21" stroke="#165DFF" strokeWidth="1.5" />
                        <path d="M8 2V6M16 2V6" stroke="#165DFF" strokeWidth="1.5" strokeLinecap="round" />
                      </svg>
                    </div>
                    <div>
                      <p className="text-[#86909c] text-[12px] mb-0.5">{t("repaymentSuccess.time")}</p>
                      <p className="text-[#1d2129] text-[15px] font-semibold">{formatDateTime(result.repaymentTime, locale)}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-[#f0f4ff] rounded-full flex items-center justify-center flex-shrink-0">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                        <rect x="2" y="5" width="20" height="14" rx="2" stroke="#165DFF" strokeWidth="1.5" />
                        <path d="M2 10H22" stroke="#165DFF" strokeWidth="1.5" />
                        <rect x="5" y="13" width="4" height="2" rx="1" fill="#165DFF" />
                      </svg>
                    </div>
                    <div>
                      <p className="text-[#86909c] text-[12px] mb-0.5">{t("repaymentSuccess.card")}</p>
                      <p className="text-[#1d2129] text-[15px] font-semibold">{formatBankCard(result.bankCard.bankName, result.bankCard.lastFour, locale)}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-[#f0fff4] rounded-full flex items-center justify-center flex-shrink-0">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                        <path d="M3 7L9 13L13 9L21 17" stroke="#22C55E" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                        <path d="M17 17H21V13" stroke="#22C55E" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    </div>
                    <div>
                      <p className="text-[#86909c] text-[12px] mb-0.5">{t("repaymentSuccess.savedInterest")}</p>
                      <p className="text-[#22c55e] text-[15px] font-semibold">{formatCurrency(result.interestSaved, locale)}</p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-2xl shadow-sm border border-[#f2f3f5] p-5">
                <div className="flex items-center gap-2 mb-3">
                  <div className="w-1 h-5 bg-[#165dff] rounded-full" />
                  <h3 className="text-[#1d2129] text-[15px] font-semibold">{t("repaymentSuccess.tipTitle")}</h3>
                </div>
                <div className="space-y-2">
                  {result.tips.map((tip, index) => (
                    <div key={`${tip}-${index}`} className="flex gap-2">
                      <span className="text-[#86909c] text-[13px] mt-0.5 flex-shrink-0">•</span>
                      <p className="text-[#86909c] text-[13px] leading-relaxed">{tip}</p>
                    </div>
                  ))}
                </div>
              </div>
            </>
          ) : null}

          {error && result ? <PageError message={error} onAction={() => void loadResult(repaymentId)} /> : null}
        </div>
      </div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 py-4 border-t border-[#f2f3f5]">
        <button
          onClick={() => {
            loan.reset();
            navigate("/");
          }}
          className="w-full h-14 bg-gradient-to-r from-[#165dff] to-[#4d8fff] rounded-full text-white text-[17px] font-semibold shadow-[0px_8px_24px_rgba(22,93,255,0.35)] active:opacity-90 transition-opacity"
        >
          {t("repaymentSuccess.backHome")}
        </button>
      </div>
    </MobileLayout>
  );
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
