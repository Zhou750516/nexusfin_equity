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
import { ChevronLeft, ChevronRight, CreditCard, Info } from "lucide-react";
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
            navigate("/calculator");
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

  const repaymentTypeLabel = info?.repaymentType === "early"
    ? t("repaymentConfirm.earlyRepayment")
    : info?.repaymentType ?? "";

  return (
    <MobileLayout>
      <div className="sticky top-0 z-10 bg-white shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)] h-[68px] flex items-center px-5">
        <button
          onClick={() => navigate(approvalResultPath)}
          aria-label={t("common.back")}
          className="absolute left-3 size-10 flex items-center justify-center"
        >
          <ChevronLeft className="size-6 text-[#1d2129]" strokeWidth={2.5} />
        </button>
        <h1 className="w-full text-center text-[#1d2129] text-lg font-semibold tracking-tight">
          {t("repaymentConfirm.title")}
        </h1>
      </div>

      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-32">
        <div className="px-5 pt-6 space-y-4">
          {info ? (
            <>
              <section
                className="rounded-2xl px-6 pt-6 pb-6 shadow-[0_10px_15px_rgba(0,0,0,0.1),0_4px_6px_rgba(0,0,0,0.1)]"
                style={{ backgroundImage: "linear-gradient(160deg, #165dff 0%, #3d8aff 100%)" }}
              >
                <p className="text-white/80 text-sm leading-[21px] tracking-tight text-center">
                  {t("repaymentConfirm.amountLabel")}
                </p>
                <p className="mt-2 text-white text-[42px] font-bold leading-[63px] tracking-[0.4px] text-center">
                  {formatCurrency(info.repaymentAmount, locale)}
                </p>
              </section>

              <section className="bg-white rounded-2xl overflow-hidden">
                <div className="flex items-center justify-between px-5 py-4 border-b border-[#e5e6eb]">
                  <span className="text-[#4e5969] text-[15px] tracking-tight">
                    {t("repaymentConfirm.method")}
                  </span>
                  <span className="text-[#1d2129] text-[15px] tracking-tight">
                    {repaymentTypeLabel}
                  </span>
                </div>
                <div className="flex items-center justify-between px-5 py-4">
                  <div className="flex items-center gap-3">
                    <div className="size-10 rounded-full bg-[#165dff]/10 flex items-center justify-center shrink-0">
                      <CreditCard className="size-5 text-[#165dff]" strokeWidth={2} />
                    </div>
                    <div>
                      <p className="text-[#1d2129] text-[15px] font-medium tracking-tight">
                        {formatBankCard(info.bankCard.bankName, info.bankCard.lastFour, locale)}
                      </p>
                      <p className="text-[#86909c] text-[13px] font-medium tracking-tight">
                        {t("repaymentConfirm.bankCard")}
                      </p>
                    </div>
                  </div>
                  <ChevronRight className="size-5 text-[#86909c]" strokeWidth={2} />
                </div>
              </section>

              <section className="bg-[#fff7e6] border border-[#ffe4b3] rounded-[14px] px-4 py-4 flex gap-3">
                <Info className="size-5 text-[#fbaf19] shrink-0 mt-0.5" strokeWidth={2} />
                <div className="flex-1 min-w-0">
                  <p className="text-[#1d2129] text-sm font-medium leading-[21px] tracking-tight">
                    {t("repaymentConfirm.tipTitle")}
                  </p>
                  <p className="mt-1 text-[#4e5969] text-[13px] leading-[21.125px]">
                    {info.tip || t("repaymentConfirm.tipBody")}
                  </p>
                </div>
              </section>
            </>
          ) : null}

          {error && info ? <PageError message={error} onAction={() => void loadInfo(loanId)} /> : null}
        </div>
      </div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white border-t border-[#e5e6eb] px-5 pt-4 pb-6">
        <button
          onClick={() => void handleSubmit()}
          disabled={isSubmitting || !info}
          className={`w-full h-14 rounded-full text-white text-[17px] font-semibold tracking-tight transition-opacity ${
            isSubmitting || !info
              ? "bg-[#c9cdd4] cursor-not-allowed"
              : "bg-gradient-to-r from-[#165dff] to-[#3d8aff] shadow-[0_20px_25px_rgba(22,93,255,0.4),0_8px_10px_rgba(22,93,255,0.4)] active:opacity-90"
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
