import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatBankCard, formatCurrency, formatDateTime } from "@/lib/format";
import { getRepaymentResult } from "@/lib/loan-api";
import { resolveRepaymentResultTime, shouldPollRepaymentResult } from "@/pages/repayment-result.logic";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { getQueryParam } from "@/lib/route";
import type { RepaymentResult } from "@/types/loan.types";
import { Calendar, CheckCircle2, CircleAlert, Clock3, CreditCard, TrendingDown, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { useLocation } from "wouter";

const MISSING_CONTEXT_COPY: Record<Locale, string> = {
  "zh-CN": "缺少还款流水号，请返回首页重新进入。",
  "zh-TW": "缺少還款流水號，請返回首頁重新進入。",
  "en-US": "Missing repayment ID. Please return to the home page and re-enter the page.",
  "vi-VN": "Thiếu mã giao dịch hoàn trả. Vui lòng quay lại trang chủ và vào lại trang này.",
};

const RESULT_COPY: Record<Locale, {
  processingTitle: string;
  processingSubtitle: string;
  failedTitle: string;
  failedSubtitle: string;
  successTitle: string;
  refresh: string;
  backHome: string;
}> = {
  "zh-CN": {
    processingTitle: "还款处理中",
    processingSubtitle: "系统已收到您的还款请求，结果确认后会自动刷新。",
    failedTitle: "还款失败",
    failedSubtitle: "本次还款暂未完成，请稍后重试或联系客服处理。",
    successTitle: "还款成功",
    refresh: "刷新结果",
    backHome: "返回首页",
  },
  "zh-TW": {
    processingTitle: "還款處理中",
    processingSubtitle: "系統已收到您的還款請求，結果確認後會自動刷新。",
    failedTitle: "還款失敗",
    failedSubtitle: "本次還款暫未完成，請稍後重試或聯繫客服處理。",
    successTitle: "還款成功",
    refresh: "刷新結果",
    backHome: "返回首頁",
  },
  "en-US": {
    processingTitle: "Repayment Processing",
    processingSubtitle: "Your repayment request has been received and this page will refresh after the final result arrives.",
    failedTitle: "Repayment Failed",
    failedSubtitle: "This repayment did not complete. Please try again later or contact support.",
    successTitle: "Repayment Successful",
    refresh: "Refresh Result",
    backHome: "Back to Home",
  },
  "vi-VN": {
    processingTitle: "Đang xử lý trả nợ",
    processingSubtitle: "Hệ thống đã nhận yêu cầu trả nợ của bạn và sẽ tự làm mới khi có kết quả cuối cùng.",
    failedTitle: "Trả nợ thất bại",
    failedSubtitle: "Lần trả nợ này chưa hoàn tất. Vui lòng thử lại sau hoặc liên hệ CSKH.",
    successTitle: "Trả nợ thành công",
    refresh: "Làm mới kết quả",
    backHome: "Về trang chủ",
  },
};

const POLL_INTERVAL_MS = 3000;

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

  useEffect(() => {
    if (!repaymentId || !result || !shouldPollRepaymentResult(result.status)) {
      return;
    }

    const timer = window.setTimeout(() => {
      void loadResult(repaymentId);
    }, POLL_INTERVAL_MS);

    return () => {
      window.clearTimeout(timer);
    };
  }, [repaymentId, result?.status, locale]);

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

  const header = resolveResultHeader(result?.status ?? "success", locale, t("repaymentSuccess.title"));
  const displayTime = result ? resolveRepaymentResultTime(result.repaymentTime, result.status) : "--";
  const primaryActionLabel = result?.status === "processing"
    ? RESULT_COPY[locale].refresh
    : RESULT_COPY[locale].backHome;

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-[#f7f8fa] via-[#fcfdfd] to-white pb-32">
        <section
          className="relative overflow-hidden px-5 pt-12 pb-24"
          style={{ backgroundImage: header.background }}
        >
          <div className="absolute -right-8 -top-8 size-64 bg-white/10 rounded-full blur-3xl" />
          <div className="absolute -left-24 bottom-0 size-48 bg-white/10 rounded-full blur-3xl" />
          <div className="relative flex flex-col items-center">
            <div className="size-20 bg-white/20 rounded-full flex items-center justify-center mb-6">
              <header.Icon className="size-10 text-white" strokeWidth={2.5} />
            </div>
            <h1 className="text-white text-[28px] font-bold leading-[42px] tracking-[0.4px] text-center">
              {header.title}
            </h1>
            <p className="mt-3 max-w-[280px] text-center text-sm leading-[21px] text-white/85">
              {header.subtitle}
            </p>
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
                    value={displayTime === "--" ? "--" : formatDateTime(displayTime, locale)}
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
                    valueClassName={result.status === "success" ? "text-[#00b42a]" : ""}
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
          onClick={() => void handlePrimaryAction({
            result,
            repaymentId,
            loan,
            navigate,
            reload: loadResult,
          })}
          className="w-full h-14 bg-gradient-to-r from-[#165dff] to-[#3d8aff] rounded-full text-white text-[17px] font-semibold tracking-tight shadow-[0_20px_25px_rgba(22,93,255,0.4),0_8px_10px_rgba(22,93,255,0.4)] active:opacity-90 transition-opacity"
        >
          {primaryActionLabel}
        </button>
      </div>
    </MobileLayout>
  );
}

async function handlePrimaryAction({
  result,
  repaymentId,
  loan,
  navigate,
  reload,
}: {
  result: RepaymentResult | null;
  repaymentId: string;
  loan: ReturnType<typeof useLoan>;
  navigate: (path: string) => void;
  reload: (repaymentId: string) => Promise<void>;
}) {
  if (result?.status === "processing") {
    await reload(repaymentId);
    return;
  }

  loan.reset();
  navigate("/calculator");
}

function resolveResultHeader(status: RepaymentResult["status"], locale: Locale, successTitle: string) {
  const copy = RESULT_COPY[locale];

  switch (status) {
    case "processing":
      return {
        title: copy.processingTitle,
        subtitle: copy.processingSubtitle,
        background: "linear-gradient(138deg, #fbaf19 0%, #ff9500 100%)",
        Icon: Clock3,
      };
    case "failed":
      return {
        title: copy.failedTitle,
        subtitle: copy.failedSubtitle,
        background: "linear-gradient(138deg, #f53f3f 0%, #ff7d00 100%)",
        Icon: XCircle,
      };
    default:
      return {
        title: successTitle || copy.successTitle,
        subtitle: "",
        background: "linear-gradient(138deg, #165dff 0%, #3d8aff 100%)",
        Icon: CheckCircle2,
      };
  }
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
