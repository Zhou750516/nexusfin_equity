import { PageError, PageLoading } from "@/components/PageFeedback";
import SectionCard from "@/components/shared/SectionCard";
import { formatCurrency, formatMonthDay } from "@/lib/format";
import type { Locale } from "@/i18n/locale";
import type { CalculateResult, RepaymentPlanItem } from "@/types/loan.types";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorRepaymentSectionProps {
  isCalculating: boolean;
  calculateResult: CalculateResult | null;
  firstRepayment: RepaymentPlanItem | null;
  expanded: boolean;
  onToggleExpanded: () => void;
  onRetry: () => void;
  locale: Locale;
  error: string | null;
  t: Translate;
}

export default function CalculatorRepaymentSection({
  isCalculating,
  calculateResult,
  firstRepayment,
  expanded,
  onToggleExpanded,
  onRetry,
  locale,
  error,
  t,
}: CalculatorRepaymentSectionProps) {
  return (
    <SectionCard title={t("calculator.repaymentTitle")} className="px-5 pb-5 pt-5">
      {isCalculating && !calculateResult ? (
        <div className="mt-3">
          <PageLoading lines={3} />
        </div>
      ) : firstRepayment && calculateResult ? (
        <>
          <div className="flex items-center justify-between border-b border-h5-border-soft pb-4 pt-4">
            <span className="text-[15px] tracking-tight text-h5-text-secondary">{t("calculator.totalFee")}</span>
            <span className="text-xl font-bold leading-[30px] tracking-tight text-[#ff6b00]">
              {formatCurrency(calculateResult.totalFee, locale)}
            </span>
          </div>
          <div className="flex items-center justify-between pt-4">
            <span className="text-[15px] tracking-tight text-h5-text-secondary">{t("calculator.firstRepayment")}</span>
            <div className="flex items-center gap-4">
              <div className="text-right">
                <p className="text-[13px] leading-5 text-h5-text-secondary">
                  {formatMonthDay(firstRepayment.date, locale)}
                </p>
                <p className="text-[17px] font-bold leading-[25.5px] tracking-tight text-h5-text-primary">
                  {formatCurrency(firstRepayment.total, locale)}
                </p>
              </div>
              <button onClick={onToggleExpanded} className="text-[13px] font-medium tracking-tight text-h5-brand">
                {expanded ? t("calculator.collapse") : t("calculator.expand")}
              </button>
            </div>
          </div>
          {expanded ? (
            <div className="mt-4 space-y-3 border-t border-h5-border-soft pt-4">
              {calculateResult.repaymentPlan.map((item) => (
                <div key={item.period} className="flex items-center justify-between">
                  <span className="text-[13px] text-h5-text-secondary">
                    {t("calculator.periodLabel", { period: item.period })}
                  </span>
                  <div className="text-right">
                    <p className="text-[12px] text-h5-text-secondary">{formatMonthDay(item.date, locale)}</p>
                    <p className="text-[14px] font-medium text-h5-text-primary">
                      {formatCurrency(item.total, locale)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </>
      ) : error ? (
        <PageError message={error} onAction={onRetry} />
      ) : null}
    </SectionCard>
  );
}
