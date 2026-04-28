import { PageError } from "@/components/PageFeedback";
import CalculatorAgreementSection from "@/components/calculator/CalculatorAgreementSection";
import CalculatorHero from "@/components/calculator/CalculatorHero";
import CalculatorLenderSection from "@/components/calculator/CalculatorLenderSection";
import CalculatorReceivingAccountSection from "@/components/calculator/CalculatorReceivingAccountSection";
import CalculatorRepaymentSection from "@/components/calculator/CalculatorRepaymentSection";
import CalculatorTermSection from "@/components/calculator/CalculatorTermSection";
import CalculatorTipsSection from "@/components/calculator/CalculatorTipsSection";
import type { Locale } from "@/i18n/locale";
import type { CalculateResult, RepaymentPlanItem, TermOption } from "@/types/loan.types";
import { PARTNERS } from "./calculatorOptions";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorPageContentProps {
  amount: number;
  locale: Locale;
  amountRangeLabel: string;
  termOptions: TermOption[];
  selectedTerm: number;
  onSelectTerm: (value: number) => void;
  isCalculating: boolean;
  calculateResult: CalculateResult | null;
  firstRepayment: RepaymentPlanItem | null;
  expanded: boolean;
  onToggleExpanded: () => void;
  annualRateValue: string;
  receivingAccountLabel: string;
  purposeLabel: string;
  partnersDialogOpen: boolean;
  onOpenPartnersDialog: (open: boolean) => void;
  onOpenAmountDrawer: () => void;
  onOpenPurposeDrawer: () => void;
  onOpenProtocolDrawer: () => void;
  onRetryCalculation: () => void;
  error: string | null;
  t: Translate;
}

export default function CalculatorPageContent({
  amount,
  locale,
  amountRangeLabel,
  termOptions,
  selectedTerm,
  onSelectTerm,
  isCalculating,
  calculateResult,
  firstRepayment,
  expanded,
  onToggleExpanded,
  annualRateValue,
  receivingAccountLabel,
  purposeLabel,
  partnersDialogOpen,
  onOpenPartnersDialog,
  onOpenAmountDrawer,
  onOpenPurposeDrawer,
  onOpenProtocolDrawer,
  onRetryCalculation,
  error,
  t,
}: CalculatorPageContentProps) {
  return (
    <div className="h5-page-shell bg-gradient-to-b from-[#f7f8fa] via-[#fcfdfd] to-white">
      <div className="space-y-4 px-5 pb-32 pt-5">
        <CalculatorHero
          amount={amount}
          locale={locale}
          amountRangeLabel={amountRangeLabel}
          onEdit={onOpenAmountDrawer}
          t={t}
        />
        <CalculatorTermSection
          termOptions={termOptions}
          selectedTerm={selectedTerm}
          onSelect={onSelectTerm}
          t={t}
        />
        <CalculatorRepaymentSection
          isCalculating={isCalculating}
          calculateResult={calculateResult}
          firstRepayment={firstRepayment}
          expanded={expanded}
          onToggleExpanded={onToggleExpanded}
          onRetry={onRetryCalculation}
          locale={locale}
          error={error}
          t={t}
        />
        <CalculatorLenderSection
          annualRateValue={annualRateValue}
          partners={PARTNERS.slice()}
          partnersDialogOpen={partnersDialogOpen}
          onOpenChange={onOpenPartnersDialog}
          t={t}
        />
        <CalculatorReceivingAccountSection receivingAccountLabel={receivingAccountLabel} t={t} />
        <CalculatorAgreementSection
          purposeLabel={purposeLabel}
          onOpenPurpose={onOpenPurposeDrawer}
          onOpenProtocol={onOpenProtocolDrawer}
          t={t}
        />
        <CalculatorTipsSection onOpenPartners={() => onOpenPartnersDialog(true)} t={t} />
        {error ? <PageError message={error} onAction={onRetryCalculation} /> : null}
      </div>
    </div>
  );
}
