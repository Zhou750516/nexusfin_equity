import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatBankCard, formatCurrency } from "@/lib/format";
import { getRepaymentInfo, submitRepayment } from "@/lib/loan-api";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath, getQueryParam } from "@/lib/route";
import type { RepaymentInfo } from "@/types/loan.types";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "wouter";

const PAY_LABEL_COPY: Record<Locale, string> = {
  "zh-CN": "确认支付",
  "zh-TW": "確認支付",
  "en-US": "Confirm Payment",
  "vi-VN": "Xác nhận thanh toán",
};

const MISSING_CONTEXT_COPY: Record<Locale, string> = {
  "zh-CN": "缺少借据编号，暂无法发起还款。",
  "zh-TW": "缺少借據編號，暫時無法發起還款。",
  "en-US": "Missing loan ID. Repayment cannot be submitted right now.",
  "vi-VN": "Thiếu mã khoản vay nên chưa thể gửi yêu cầu hoàn trả.",
};

const MISSING_BANK_CARD_COPY: Record<Locale, string> = {
  "zh-CN": "缺少还款银行卡信息，请稍后重试。",
  "zh-TW": "缺少還款銀行卡資訊，請稍後重試。",
  "en-US": "Repayment card information is missing. Please try again later.",
  "vi-VN": "Thiếu thông tin thẻ trả nợ. Vui lòng thử lại sau.",
};

export default function ConfirmRepaymentPage() {
  const [, navigate] = useLocation();
  const loan = useLoan();
  const { locale, t } = useI18n();
  const [info, setInfo] = useState<RepaymentInfo | null>(null);
  const [loadedLocale, setLoadedLocale] = useState<Locale | null>(null);
  const [loadedLoanId, setLoadedLoanId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loanId = getQueryParam("loanId") ?? loan.loanId;
  const approvalResultPath = loan.applicationId
    ? buildPath("/approval-result", { applicationId: loan.applicationId })
    : "/approval-result";

  useEffect(() => {
    if (loanId && loanId !== loan.loanId) {
      loan.setLoanId(loanId);
    }
  }, [loanId]);

  useEffect(() => {
    if (!loanId) {
      setIsLoading(false);
      return;
    }
    if (!shouldRequestLocalizedData({
      locale,
      loadedLocale,
      requestKey: loanId,
      loadedRequestKey: loadedLoanId,
    })) {
      return;
    }
    void loadInfo(loanId);
  }, [loadedLoanId, loadedLocale, loanId, locale]);

  async function loadInfo(currentLoanId: string) {
    setIsLoading(true);
    setError(null);
    try {
      const nextInfo = await getRepaymentInfo(currentLoanId);
      setInfo(nextInfo);
      setLoadedLocale(locale);
      setLoadedLoanId(currentLoanId);
    } catch (loadError) {
      setError(readErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSubmit() {
    if (!info) {
      return;
    }

    const bankCardId = info.bankCard.accountId ?? loan.receivingAccountId;
    if (!bankCardId) {
      setError(MISSING_BANK_CARD_COPY[locale]);
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      const response = await submitRepayment({
        loanId: info.loanId,
        amount: info.repaymentAmount,
        bankCardId,
        repaymentType: "early",
      });
      loan.setRepaymentId(response.repaymentId);
      navigate(buildPath("/repayment-success", { repaymentId: response.repaymentId }));
    } catch (submitError) {
      setError(readErrorMessage(submitError));
    } finally {
      setIsSubmitting(false);
    }
  }

  const payButtonText = useMemo(() => {
    if (!info) {
      return PAY_LABEL_COPY[locale];
    }
    return `${PAY_LABEL_COPY[locale]} ${formatCurrency(info.repaymentAmount, locale)}`;
  }, [info, locale]);

  if (!loanId) {
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

  if (isLoading && !info) {
    return (
      <MobileLayout>
        <PageLoading lines={4} />
      </MobileLayout>
    );
  }

  if (error && !info) {
    return (
      <MobileLayout>
        <PageError message={error} onAction={() => void loadInfo(loanId)} />
      </MobileLayout>
    );
  }

  return (
    <MobileLayout>
      <div className="sticky top-0 z-10 bg-white border-b border-[#f2f3f5] h-[54px] flex items-center px-5">
        <button onClick={() => navigate(approvalResultPath)} className="absolute left-5">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M15 18L9 12L15 6" stroke="#1d2129" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
        <h1 className="w-full text-center text-[#1d2129] text-[17px] font-semibold">{t("repaymentConfirm.title")}</h1>
      </div>

      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-24">
        <div className="px-4 pt-4 space-y-3">
          {info ? (
            <>
              <div className="bg-gradient-to-br from-[#165dff] via-[#1a65ff] to-[#4d8fff] rounded-2xl p-6 text-center shadow-[0px_8px_24px_rgba(22,93,255,0.25)]">
                <p className="text-white/80 text-[13px] mb-2">{t("repaymentConfirm.amountLabel")}</p>
                <div className="flex items-baseline justify-center gap-1">
                  <span className="text-white text-[22px] font-medium">¥</span>
                  <span className="text-white text-[48px] font-bold leading-none">{formatCurrency(info.repaymentAmount, locale, { includeSymbol: false })}</span>
                </div>
              </div>

              <div className="bg-white rounded-2xl shadow-sm border border-[#f2f3f5] overflow-hidden">
                <div className="flex justify-between items-center px-5 py-4 border-b border-[#f2f3f5]">
                  <span className="text-[#4e5969] text-[15px]">{t("repaymentConfirm.method")}</span>
                  <span className="text-[#165dff] text-[15px] font-medium">{info.repaymentType}</span>
                </div>
                <div className="w-full flex items-center gap-3 px-5 py-4">
                  <div className="w-10 h-10 bg-[#f0f4ff] rounded-full flex items-center justify-center flex-shrink-0">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                      <rect x="2" y="5" width="20" height="14" rx="2" stroke="#165DFF" strokeWidth="1.5" />
                      <path d="M2 10H22" stroke="#165DFF" strokeWidth="1.5" />
                      <rect x="5" y="13" width="4" height="2" rx="1" fill="#165DFF" />
                    </svg>
                  </div>
                  <div className="flex-1 text-left">
                    <p className="text-[#1d2129] text-[15px] font-semibold">{formatBankCard(info.bankCard.bankName, info.bankCard.lastFour, locale)}</p>
                    <p className="text-[#86909c] text-[13px]">{t("repaymentConfirm.bankCard")}</p>
                  </div>
                </div>
              </div>

              <div className="bg-[#fffbf0] rounded-2xl border border-[#ffe8b8] p-5">
                <div className="flex gap-3">
                  <div className="flex-shrink-0 mt-0.5">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="10" stroke="#FF9500" strokeWidth="2" />
                      <path d="M12 8V12" stroke="#FF9500" strokeWidth="2" strokeLinecap="round" />
                      <circle cx="12" cy="16" r="1" fill="#FF9500" />
                    </svg>
                  </div>
                  <div>
                    <h4 className="text-[#8b4513] text-[13px] font-semibold mb-1">{t("repaymentConfirm.tipTitle")}</h4>
                    <p className="text-[#8b4513] text-[13px] leading-relaxed">{info.tip}</p>
                  </div>
                </div>
              </div>
            </>
          ) : null}

          {error && info ? <PageError message={error} onAction={() => void loadInfo(loanId)} /> : null}
        </div>
      </div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 py-4 border-t border-[#f2f3f5]">
        <button
          onClick={() => void handleSubmit()}
          disabled={isSubmitting || !info}
          className={`w-full h-14 rounded-full text-white text-[17px] font-semibold transition-opacity ${
            isSubmitting || !info
              ? "bg-[#c9cdd4] cursor-not-allowed"
              : "bg-gradient-to-r from-[#165dff] to-[#4d8fff] shadow-[0px_8px_24px_rgba(22,93,255,0.35)] active:opacity-90"
          }`}
        >
          {isSubmitting ? `${PAY_LABEL_COPY[locale]}...` : payButtonText}
        </button>
      </div>
    </MobileLayout>
  );
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
