import { formatCurrency } from "@/lib/format";
import type { Locale } from "@/i18n/locale";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorHeroProps {
  amount: number;
  locale: Locale;
  amountRangeLabel: string;
  onEdit: () => void;
  t: Translate;
}

export default function CalculatorHero({
  amount,
  locale,
  amountRangeLabel,
  onEdit,
  t,
}: CalculatorHeroProps) {
  return (
    <div
      className="relative overflow-hidden rounded-3xl shadow-[0_20px_25px_-5px_rgba(22,93,255,0.2),0_8px_10px_-6px_rgba(22,93,255,0.2)]"
      style={{ backgroundImage: "linear-gradient(153deg, #165dff 0%, #3d8aff 100%)" }}
    >
      <div className="absolute -left-8 bottom-0 h-24 w-24 rounded-full bg-white/10 blur-[40px]" />
      <div className="relative px-8 py-8">
        <p className="text-sm leading-[21px] tracking-tight text-white/80">{t("calculator.loanAmount")}</p>
        <div className="mt-3 flex items-end gap-3 border-b border-white pb-3">
          <span className="mb-1 text-2xl font-medium leading-9 text-white">¥</span>
          <span className="text-[64px] font-bold leading-[64px] tracking-[0.2px] text-white">
            {formatCurrency(amount, locale, { includeSymbol: false })}
          </span>
          <button className="mb-1 ml-auto text-[13px] tracking-tight text-white/60" onClick={onEdit}>
            {t("calculator.editAmount")}
          </button>
        </div>
        <div className="mt-3 flex items-center gap-2">
          <span className="h-1 w-1 rounded-full bg-white/50" />
          <span className="text-[13px] leading-5 tracking-tight text-white/70">{amountRangeLabel}</span>
        </div>
      </div>
    </div>
  );
}
