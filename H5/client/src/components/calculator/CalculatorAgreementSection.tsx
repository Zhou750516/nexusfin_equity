import { ChevronRight } from "lucide-react";
import SectionCard from "@/components/shared/SectionCard";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorAgreementSectionProps {
  purposeLabel: string;
  onOpenPurpose: () => void;
  onOpenProtocol: () => void;
  t: Translate;
}

function ActionRow({
  label,
  value,
  onClick,
}: {
  label: string;
  value: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-center justify-between rounded-2xl border border-h5-border-soft bg-h5-surface-strong px-5 py-5 text-left shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)]"
    >
      <span className="text-[15px] tracking-tight text-h5-text-secondary">{label}</span>
      <div className="flex items-center gap-2">
        <span className="text-[15px] font-medium tracking-tight text-h5-text-primary">{value}</span>
        <ChevronRight className="size-4 text-h5-text-secondary" strokeWidth={2} />
      </div>
    </button>
  );
}

export default function CalculatorAgreementSection({
  purposeLabel,
  onOpenPurpose,
  onOpenProtocol,
  t,
}: CalculatorAgreementSectionProps) {
  return (
    <div className="space-y-4">
      <ActionRow label={t("calculator.loanPurpose")} value={purposeLabel} onClick={onOpenPurpose} />
      <button
        type="button"
        onClick={onOpenProtocol}
        className="flex w-full items-center justify-between rounded-2xl border border-h5-border-soft bg-h5-surface-strong px-5 py-5 text-left shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)]"
      >
        <span className="text-[15px] tracking-tight text-h5-text-secondary">{t("calculator.loanProtocol")}</span>
        <span className="text-[15px] font-medium tracking-tight text-h5-brand">
          {t("calculator.loanProtocolView")}
        </span>
      </button>
    </div>
  );
}
