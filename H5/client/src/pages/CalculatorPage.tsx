import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import CalculatorAmountDrawer from "@/components/calculator/CalculatorAmountDrawer";
import CalculatorPageContent from "@/components/calculator/CalculatorPageContent";
import CalculatorProtocolDrawer from "@/components/calculator/CalculatorProtocolDrawer";
import CalculatorPurposeDrawer from "@/components/calculator/CalculatorPurposeDrawer";
import {
  LOAN_PURPOSE_KEYS,
  PROTOCOL_KEYS,
} from "@/components/calculator/calculatorOptions";
import StickyActionBar from "@/components/shared/StickyActionBar";
import { useCalculatorPageState } from "./useCalculatorPageState";

export default function CalculatorPage() {
  const {
    locale,
    t,
    config,
    amount,
    selectedTerm,
    draftAmount,
    purposeKey,
    expanded,
    drawerOpen,
    purposeDrawerOpen,
    protocolDrawerOpen,
    viewedProtocols,
    partnersDialogOpen,
    isLoading,
    isCalculating,
    isSubmitting,
    error,
    allProtocolsAgreed,
    calculateResult,
    firstRepayment,
    annualRateValue,
    receivingAccountLabel,
    amountRangeLabel,
    drawerStep,
    isSubmitDisabled,
    drawerQuickActions,
    setSelectedTerm,
    setDraftAmount,
    setExpanded,
    setDrawerOpen,
    setPurposeDrawerOpen,
    setProtocolDrawerOpen,
    setPartnersDialogOpen,
    loadConfig,
    loadCalculation,
    handleSubmit,
    confirmDraftAmount,
    selectPurpose,
    viewProtocol,
  } = useCalculatorPageState();

  if (isLoading && !config) {
    return (
      <MobileLayout>
        <PageLoading lines={6} />
      </MobileLayout>
    );
  }

  if (error && !config) {
    return (
      <MobileLayout>
        <PageError message={error} onAction={() => void loadConfig()} />
      </MobileLayout>
    );
  }

  return (
    <MobileLayout>
      <div className="flex h-[55px] items-center border-b border-h5-border-soft bg-white/80 px-5 backdrop-blur-sm">
        <button
          className="text-[15px] font-medium tracking-tight text-h5-text-primary"
          onClick={() => history.back()}
        >
          {t("common.back")}
        </button>
      </div>

      <CalculatorPageContent
        amount={amount}
        locale={locale}
        amountRangeLabel={amountRangeLabel}
        termOptions={config?.termOptions ?? []}
        selectedTerm={selectedTerm}
        onSelectTerm={setSelectedTerm}
        isCalculating={isCalculating}
        calculateResult={calculateResult}
        firstRepayment={firstRepayment}
        expanded={expanded}
        onToggleExpanded={() => setExpanded((previous) => !previous)}
        annualRateValue={annualRateValue}
        receivingAccountLabel={receivingAccountLabel}
        purposeLabel={t(purposeKey)}
        partnersDialogOpen={partnersDialogOpen}
        onOpenPartnersDialog={setPartnersDialogOpen}
        onOpenAmountDrawer={() => setDrawerOpen(true)}
        onOpenPurposeDrawer={() => setPurposeDrawerOpen(true)}
        onOpenProtocolDrawer={() => setProtocolDrawerOpen(true)}
        onRetryCalculation={() => void loadCalculation(amount, selectedTerm)}
        error={error && config ? error : null}
        t={t}
      />

      <StickyActionBar
        primary={
          <button
            onClick={() => void handleSubmit()}
            disabled={isSubmitDisabled}
            className={`h-14 w-full rounded-full text-[17px] font-semibold tracking-tight text-white transition-opacity ${
              isSubmitDisabled
                ? "cursor-not-allowed bg-[#c9cdd4]"
                : "bg-gradient-to-r from-h5-brand to-h5-brand-strong active:opacity-90"
            }`}
          >
            {isSubmitting ? `${t("calculator.submit")}...` : t("calculator.submit")}
          </button>
        }
      />

      <CalculatorAmountDrawer
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        amountRangeLabel={amountRangeLabel}
        drawerStep={drawerStep}
        amountRangeMin={config?.amountRange.min}
        amountRangeMax={config?.amountRange.max}
        draftAmount={draftAmount}
        drawerQuickActions={drawerQuickActions}
        locale={locale}
        onDraftAmountChange={setDraftAmount}
        onConfirm={confirmDraftAmount}
        t={t}
      />

      <CalculatorPurposeDrawer
        open={purposeDrawerOpen}
        onOpenChange={setPurposeDrawerOpen}
        purposeKey={purposeKey}
        loanPurposeKeys={LOAN_PURPOSE_KEYS}
        onSelect={selectPurpose}
        t={t}
      />

      <CalculatorProtocolDrawer
        open={protocolDrawerOpen}
        onOpenChange={setProtocolDrawerOpen}
        protocolKeys={PROTOCOL_KEYS}
        allProtocolsAgreed={allProtocolsAgreed}
        viewedProtocolCount={viewedProtocols.size}
        onViewProtocol={viewProtocol}
        t={t}
      />
    </MobileLayout>
  );
}
