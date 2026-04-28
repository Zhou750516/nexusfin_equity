import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { Input } from "@/components/ui/input";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatBankCard, formatCurrency } from "@/lib/format";
import {
  confirmRepaymentSms,
  getRepaymentInfo,
  sendRepaymentSms,
  submitRepayment,
} from "@/lib/loan-api";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath, getQueryParam } from "@/lib/route";
import {
  canProceedRepaymentAction,
  resolveRepaymentActionStage,
  resolveSelectedRepaymentCardId,
} from "@/pages/confirm-repayment.logic";
import type { RepaymentInfo } from "@/types/loan.types";
import { CheckCircle2, ChevronLeft, CreditCard, Info } from "lucide-react";
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

const SMS_SECTION_TITLE: Record<Locale, string> = {
  "zh-CN": "短信验证",
  "zh-TW": "簡訊驗證",
  "en-US": "SMS Verification",
  "vi-VN": "Xác minh SMS",
};

const SMS_SECTION_HINT: Record<Locale, string> = {
  "zh-CN": "根据后端要求，提交还款前需要先发送短信并校验验证码。",
  "zh-TW": "依照後端要求，送出還款前需先發送簡訊並校驗驗證碼。",
  "en-US": "Backend rules require sending an SMS code and verifying it before repayment submission.",
  "vi-VN": "Theo yêu cầu backend, cần gửi SMS và xác minh mã trước khi gửi yêu cầu trả nợ.",
};

const SMS_INPUT_PLACEHOLDER: Record<Locale, string> = {
  "zh-CN": "请输入短信验证码",
  "zh-TW": "請輸入簡訊驗證碼",
  "en-US": "Enter the SMS code",
  "vi-VN": "Nhập mã xác minh SMS",
};

const SMS_STATUS_COPY: Record<Locale, Record<"idle" | "sent" | "verified", string>> = {
  "zh-CN": {
    idle: "待发送",
    sent: "已发送",
    verified: "已验证",
  },
  "zh-TW": {
    idle: "待發送",
    sent: "已發送",
    verified: "已驗證",
  },
  "en-US": {
    idle: "Pending",
    sent: "Sent",
    verified: "Verified",
  },
  "vi-VN": {
    idle: "Chờ gửi",
    sent: "Đã gửi",
    verified: "Đã xác minh",
  },
};

const ACTION_COPY: Record<Locale, Record<"send_sms" | "confirm_sms" | "submit", string>> = {
  "zh-CN": {
    send_sms: "发送验证码",
    confirm_sms: "验证并支付",
    submit: "确认支付",
  },
  "zh-TW": {
    send_sms: "發送驗證碼",
    confirm_sms: "驗證並支付",
    submit: "確認支付",
  },
  "en-US": {
    send_sms: "Send Code",
    confirm_sms: "Verify & Pay",
    submit: "Confirm Payment",
  },
  "vi-VN": {
    send_sms: "Gửi mã",
    confirm_sms: "Xác minh & thanh toán",
    submit: "Xác nhận thanh toán",
  },
};

const ACTION_PENDING_COPY: Record<Locale, Record<"send_sms" | "confirm_sms" | "submit", string>> = {
  "zh-CN": {
    send_sms: "发送中...",
    confirm_sms: "验证中...",
    submit: "支付中...",
  },
  "zh-TW": {
    send_sms: "發送中...",
    confirm_sms: "驗證中...",
    submit: "支付中...",
  },
  "en-US": {
    send_sms: "Sending...",
    confirm_sms: "Verifying...",
    submit: "Processing...",
  },
  "vi-VN": {
    send_sms: "Đang gửi...",
    confirm_sms: "Đang xác minh...",
    submit: "Đang xử lý...",
  },
};

export default function ConfirmRepaymentPage() {
  const [, navigate] = useLocation();
  const loan = useLoan();
  const { locale, t } = useI18n();
  const [info, setInfo] = useState<RepaymentInfo | null>(null);
  const [loadedLocale, setLoadedLocale] = useState<Locale | null>(null);
  const [loadedLoanId, setLoadedLoanId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedBankCardId, setSelectedBankCardId] = useState<string | null>(null);
  const [smsCaptcha, setSmsCaptcha] = useState("");
  const [smsSeq, setSmsSeq] = useState<string | null>(null);
  const [smsVerified, setSmsVerified] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [isSendingSms, setIsSendingSms] = useState(false);
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
      setSelectedBankCardId((currentSelectedCardId) => resolveSelectedRepaymentCardId(
        currentSelectedCardId,
        nextInfo.bankCard.accountId,
        nextInfo.bankCards,
      ));
      setLoadedLocale(locale);
      setLoadedLoanId(currentLoanId);
      setSmsCaptcha("");
      setSmsSeq(null);
      setSmsVerified(false);
      setStatusMessage(null);
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

    const bankCardId = selectedCardId;
    if (!bankCardId) {
      setError(MISSING_BANK_CARD_COPY[locale]);
      return;
    }

    setError(null);
    if (actionStage === "send_sms") {
      setIsSendingSms(true);
      try {
        const response = await sendRepaymentSms({
          loanId: info.loanId,
          bankCardId,
        });
        setSmsSeq(response.smsSeq);
        setSmsVerified(false);
        setStatusMessage(response.message);
        return;
      } catch (sendError) {
        setError(readErrorMessage(sendError));
        return;
      } finally {
        setIsSendingSms(false);
      }
    }

    setIsSubmitting(true);
    try {
      if (actionStage === "confirm_sms") {
        const response = await confirmRepaymentSms({
          loanId: info.loanId,
          captcha: smsCaptcha.trim(),
        });
        setSmsVerified(response.status === "confirmed");
        setStatusMessage(response.message);
      }

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

  const selectedBankCard = info?.bankCards.find((card) => card.accountId === selectedBankCardId)
    ?? info?.bankCard
    ?? null;
  const selectedCardId = selectedBankCardId ?? selectedBankCard?.accountId ?? loan.receivingAccountId ?? null;
  const actionStage = resolveRepaymentActionStage({
    hasBankCard: Boolean(selectedCardId),
    smsRequired: Boolean(info?.smsRequired),
    smsSent: Boolean(smsSeq),
    smsVerified,
    captcha: smsCaptcha,
  });
  const canProceed = info
    ? canProceedRepaymentAction({
      hasBankCard: Boolean(selectedCardId),
      smsRequired: info.smsRequired,
      smsSent: Boolean(smsSeq),
      smsVerified,
      captcha: smsCaptcha,
    })
    : false;
  const isBusy = isSendingSms || isSubmitting;

  const payButtonText = useMemo(() => {
    if (!info) {
      return ACTION_COPY[locale].submit;
    }
    if (actionStage !== "submit") {
      return ACTION_COPY[locale][actionStage];
    }
    return `${ACTION_COPY[locale].submit} ${formatCurrency(info.repaymentAmount, locale)}`;
  }, [actionStage, info, locale]);

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
  const smsStatus = smsVerified ? "verified" : smsSeq ? "sent" : "idle";

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
                <div className="px-5 py-4">
                  <div className="flex flex-col gap-3">
                    {info.bankCards.map((card) => {
                      const cardId = card.accountId ?? null;
                      const isSelected = cardId === selectedCardId;
                      return (
                        <button
                          key={cardId ?? `${card.bankName}-${card.lastFour}`}
                          type="button"
                          onClick={() => {
                            setSelectedBankCardId(cardId);
                            setSmsSeq(null);
                            setSmsVerified(false);
                            setSmsCaptcha("");
                            setStatusMessage(null);
                          }}
                          className={`w-full rounded-2xl border px-4 py-3 text-left transition-colors ${
                            isSelected
                              ? "border-[#165dff] bg-[#165dff]/5"
                              : "border-[#e5e6eb] bg-[#f7f8fa]"
                          }`}
                        >
                          <div className="flex items-center justify-between gap-3">
                            <div className="flex items-center gap-3 min-w-0">
                              <div className="size-10 rounded-full bg-[#165dff]/10 flex items-center justify-center shrink-0">
                                <CreditCard className="size-5 text-[#165dff]" strokeWidth={2} />
                              </div>
                              <div className="min-w-0">
                                <p className="text-[#1d2129] text-[15px] font-medium tracking-tight">
                                  {formatBankCard(card.bankName, card.lastFour, locale)}
                                </p>
                                <p className="text-[#86909c] text-[13px] font-medium tracking-tight">
                                  {t("repaymentConfirm.bankCard")}
                                </p>
                              </div>
                            </div>
                            <span className={`size-5 rounded-full border flex items-center justify-center ${
                              isSelected ? "border-[#165dff] bg-[#165dff]" : "border-[#c9cdd4] bg-white"
                            }`}>
                              {isSelected ? <CheckCircle2 className="size-4 text-white" strokeWidth={2.5} /> : null}
                            </span>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </section>

              {info.smsRequired ? (
                <section className="bg-white rounded-2xl shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)] px-5 pt-5 pb-5">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-[#1d2129] text-base font-semibold tracking-tight">
                        {SMS_SECTION_TITLE[locale]}
                      </p>
                      <p className="mt-1 text-[#86909c] text-[13px] leading-[21.125px]">
                        {statusMessage ?? SMS_SECTION_HINT[locale]}
                      </p>
                    </div>
                    <span className="shrink-0 rounded-full bg-[#165dff]/10 px-3 py-1 text-xs font-medium text-[#165dff]">
                      {SMS_STATUS_COPY[locale][smsStatus]}
                    </span>
                  </div>

                  <div className="mt-4">
                    <Input
                      value={smsCaptcha}
                      onChange={(event) => setSmsCaptcha(event.target.value)}
                      inputMode="numeric"
                      maxLength={6}
                      placeholder={SMS_INPUT_PLACEHOLDER[locale]}
                      disabled={smsVerified || isBusy}
                      className="h-12 rounded-xl border-[#d9dde4] bg-[#f7f8fa] px-4 text-[15px]"
                    />
                  </div>
                </section>
              ) : null}

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
          disabled={isBusy || !info || !canProceed}
          className={`w-full h-14 rounded-full text-white text-[17px] font-semibold tracking-tight transition-opacity ${
            isBusy || !info || !canProceed
              ? "bg-[#c9cdd4] cursor-not-allowed"
              : "bg-gradient-to-r from-[#165dff] to-[#3d8aff] shadow-[0_20px_25px_rgba(22,93,255,0.4),0_8px_10px_rgba(22,93,255,0.4)] active:opacity-90"
          }`}
        >
          {isBusy ? ACTION_PENDING_COPY[locale][actionStage] : payButtonText}
        </button>
      </div>
    </MobileLayout>
  );
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
