import SectionCard from "@/components/shared/SectionCard";
import type { TermOption } from "@/types/loan.types";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorTermSectionProps {
  termOptions: TermOption[];
  selectedTerm: number;
  onSelect: (value: number) => void;
  t: Translate;
}

export default function CalculatorTermSection({
  termOptions,
  selectedTerm,
  onSelect,
  t,
}: CalculatorTermSectionProps) {
  return (
    <SectionCard title={t("calculator.borrowDuration")}>
      <div className="mt-4 flex gap-3">
        {termOptions.map((option) => (
          <button
            key={option.value}
            onClick={() => onSelect(option.value)}
            className={`h-11 flex-1 rounded-[10px] border text-[15px] font-medium tracking-tight transition-all ${
              selectedTerm === option.value
                ? "border-h5-brand bg-h5-brand/8 text-h5-brand"
                : "border-h5-border-soft bg-h5-surface-strong text-h5-text-secondary"
            }`}
          >
            {option.label}
          </button>
        ))}
      </div>
    </SectionCard>
  );
}
