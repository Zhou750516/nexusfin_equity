import SectionCard from "@/components/shared/SectionCard";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorReceivingAccountSectionProps {
  receivingAccountLabel: string;
  t: Translate;
}

export default function CalculatorReceivingAccountSection({
  receivingAccountLabel,
  t,
}: CalculatorReceivingAccountSectionProps) {
  return (
    <SectionCard className="py-5">
      <div className="flex items-center justify-between">
        <span className="text-[15px] tracking-tight text-h5-text-secondary">{t("calculator.receivingAccount")}</span>
        <span className="text-[15px] font-medium tracking-tight text-h5-text-primary">
          {receivingAccountLabel}
        </span>
      </div>
    </SectionCard>
  );
}
