import SectionCard from "@/components/shared/SectionCard";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorTipsSectionProps {
  onOpenPartners: () => void;
  t: Translate;
}

export default function CalculatorTipsSection({
  onOpenPartners,
  t,
}: CalculatorTipsSectionProps) {
  return (
    <SectionCard className="border-[#e3e4eb] bg-h5-surface py-5">
      <p className="text-[15px] font-medium tracking-tight text-h5-text-primary">{t("calculator.tipsTitle")}</p>
      <div className="mt-4 space-y-3 text-[13px] leading-[21px] tracking-tight text-h5-text-secondary">
        <p>
          {t("calculator.tip1Prefix")}
          <button type="button" onClick={onOpenPartners} className="text-h5-brand">
            {t("calculator.tip1Highlight")}
          </button>
          {t("calculator.tip1Suffix")}
        </p>
        <p>{t("calculator.tip2")}</p>
        <p>{t("calculator.tip3")}</p>
      </div>
    </SectionCard>
  );
}
