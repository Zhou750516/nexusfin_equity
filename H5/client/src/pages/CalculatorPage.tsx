import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { calculateLoan, getCalculatorConfig, applyLoan } from "@/lib/loan-api";
import { formatBankCard, formatCurrency, formatMonthDay } from "@/lib/format";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath } from "@/lib/route";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import type { AmountRange, CalculateResult, CalculatorConfig } from "@/types/loan.types";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "wouter";

const AGREED_PROTOCOLS = ["user_agreement", "loan_agreement", "privacy_policy"];

export default function CalculatorPage() {
  const [, navigate] = useLocation();
  const loan = useLoan();
  const { locale, t } = useI18n();
  const [config, setConfig] = useState<CalculatorConfig | null>(null);
  const [calculateResult, setCalculateResult] = useState<CalculateResult | null>(null);
  const [loadedConfigLocale, setLoadedConfigLocale] = useState<Locale | null>(null);
  const [amount, setAmount] = useState(loan.amount);
  const [selectedTerm, setSelectedTerm] = useState(loan.term);
  const [draftAmount, setDraftAmount] = useState(String(loan.amount));
  const [agreed, setAgreed] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isCalculating, setIsCalculating] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const agreements = [t("calculator.userAgreement"), t("calculator.loanAgreement"), t("calculator.privacyPolicy")];
  const agreementSeparator = locale.startsWith("zh") ? "、" : " · ";

  useEffect(() => {
    if (!shouldRequestLocalizedData({ locale, loadedLocale: loadedConfigLocale })) {
      return;
    }
    void loadConfig();
  }, [loadedConfigLocale, locale]);

  useEffect(() => {
    if (!config) {
      return;
    }

    const timer = window.setTimeout(() => {
      void loadCalculation(amount, selectedTerm);
    }, 300);

    return () => {
      window.clearTimeout(timer);
    };
  }, [config, amount, selectedTerm]);

  async function loadConfig() {
    setIsLoading(true);
    setError(null);
    try {
      const nextConfig = await getCalculatorConfig();
      setConfig(nextConfig);
      setLoadedConfigLocale(locale);
      loan.setReceivingAccountId(nextConfig.receivingAccount.accountId ?? null);

      const normalizedAmount = normalizeAmount(loan.amount, nextConfig.amountRange);
      const supportedTerms = nextConfig.termOptions.map((option) => option.value);
      const normalizedTerm = supportedTerms.includes(loan.term) ? loan.term : (nextConfig.termOptions[0]?.value ?? 3);

      setAmount(normalizedAmount);
      setSelectedTerm(normalizedTerm);
      setDraftAmount(String(normalizedAmount));
      loan.setAmount(normalizedAmount);
      loan.setTerm(normalizedTerm);
    } catch (loadError) {
      setError(readErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  }

  async function loadCalculation(nextAmount: number, nextTerm: number) {
    setIsCalculating(true);
    setError(null);
    try {
      const nextResult = await calculateLoan({ amount: nextAmount, term: nextTerm });
      setCalculateResult(nextResult);
      loan.setAmount(nextAmount);
      loan.setTerm(nextTerm);
    } catch (loadError) {
      setCalculateResult(null);
      setError(readErrorMessage(loadError));
    } finally {
      setIsCalculating(false);
    }
  }

  async function handleSubmit() {
    if (!config || !calculateResult) {
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      const result = await applyLoan({
        amount,
        term: selectedTerm,
        receivingAccountId: config.receivingAccount.accountId ?? loan.receivingAccountId ?? "",
        agreedProtocols: AGREED_PROTOCOLS,
      });

      loan.setApplicationId(result.applicationId);
      loan.setApprovalStatus(result.status === "loan_failed" ? "loan_failed" : "pending");
      loan.setApprovalMessage(result.message ?? null);
      loan.setBenefitsCardActivated(result.benefitsActivated);
      loan.setBenefitOrderNo(result.benefitOrderNo ?? null);

      if (result.status === "loan_failed") {
        navigate("/approval-result");
        return;
      }

      if (result.applicationId) {
        navigate(buildPath("/approval-pending", { applicationId: result.applicationId }));
      }
    } catch (submitError) {
      setError(readErrorMessage(submitError));
    } finally {
      setIsSubmitting(false);
    }
  }

  const firstRepayment = calculateResult?.repaymentPlan[0] ?? null;
  const annualRate = calculateResult?.annualRate ?? (config ? `${(config.annualRate * 100).toFixed(1)}%` : "--");
  const receivingAccountLabel = config
    ? formatBankCard(config.receivingAccount.bankName, config.receivingAccount.lastFour, locale)
    : "--";
  const amountRangeLabel = config
    ? `${formatCurrency(config.amountRange.min, locale)} - ${formatCurrency(config.amountRange.max, locale)}`
    : t("calculator.amountRange");
  const drawerStep = config?.amountRange.step ?? 100;

  const isSubmitDisabled = !agreed || !calculateResult || isSubmitting || isCalculating;

  const drawerQuickActions = useMemo(() => {
    if (!config) {
      return [];
    }

    return [
      config.amountRange.min,
      config.amountRange.default,
      config.amountRange.max,
    ];
  }, [config]);

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
      <div className="bg-white h-[54px] flex items-center px-5 border-b border-[#f2f3f5]">
        <button className="flex items-center gap-1 text-[#1d2129] text-[17px] font-medium" onClick={() => history.back()}>
          <svg width="8" height="14" viewBox="0 0 8 14" fill="none">
            <path d="M7 1L1 7L7 13" stroke="#1d2129" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          <span className="ml-1">{t("common.back")}</span>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-24">
        <div className="px-4 pt-4 space-y-3">
          <div className="relative bg-gradient-to-br from-[#165dff] via-[#1a65ff] to-[#4d8fff] rounded-2xl shadow-[0px_8px_24px_rgba(22,93,255,0.25)] overflow-hidden">
            <div className="absolute w-32 h-32 right-[-10px] top-[-10px] bg-white/10 rounded-full blur-3xl" />
            <div className="relative px-6 pt-5 pb-5">
              <p className="text-white/80 text-[13px] font-normal mb-3">{t("calculator.loanAmount")}</p>
              <div className="flex items-end gap-2 border-b border-white/30 pb-3 mb-3">
                <span className="text-white text-[22px] font-medium leading-none mb-1">¥</span>
                <span className="text-white text-[56px] font-bold leading-none">{formatCurrency(amount, locale, { includeSymbol: false })}</span>
                <button className="ml-auto text-white/70 text-[13px] font-normal pb-1" onClick={() => setDrawerOpen(true)}>
                  {t("calculator.editAmount")}
                </button>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-1.5 bg-white/50 rounded-full" />
                <span className="text-white/70 text-[13px]">{amountRangeLabel}</span>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-[#f2f3f5] px-5 pt-5 pb-4">
            <h3 className="text-[#1d2129] text-[15px] font-semibold mb-4">{t("calculator.borrowDuration")}</h3>
            <div className="flex gap-3">
              {config?.termOptions.map((option) => (
                <button
                  key={option.value}
                  onClick={() => setSelectedTerm(option.value)}
                  className={`flex-1 h-11 rounded-xl text-[15px] font-medium transition-all ${
                    selectedTerm === option.value
                      ? "bg-[#f0f4ff] text-[#165dff] border border-[#165dff]"
                      : "bg-white text-[#4e5969] border border-[#e5e6eb]"
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-[#f2f3f5] px-5 pt-5 pb-5">
            <h3 className="text-[#1d2129] text-[17px] font-bold mb-4">{t("calculator.repaymentTitle")}</h3>
            {isCalculating && !calculateResult ? (
              <PageLoading lines={3} />
            ) : firstRepayment && calculateResult ? (
              <>
                <div className="flex justify-between items-center py-4 border-b border-[#f2f3f5]">
                  <span className="text-[#4e5969] text-[15px]">{t("calculator.totalFee")}</span>
                  <span className="text-[#ff6b00] text-[20px] font-bold">{formatCurrency(calculateResult.totalFee, locale)}</span>
                </div>
                <div className="flex justify-between items-center pt-4">
                  <span className="text-[#4e5969] text-[15px]">{t("calculator.firstRepayment")}</span>
                  <div className="flex items-center gap-3">
                    <div className="text-right">
                      <p className="text-[#86909c] text-[13px]">{formatMonthDay(firstRepayment.date, locale)}</p>
                      <p className="text-[#1d2129] text-[17px] font-bold">{formatCurrency(firstRepayment.total, locale)}</p>
                    </div>
                    <button onClick={() => setExpanded((previous) => !previous)} className="text-[#165dff] text-[13px] font-medium">
                      {expanded ? t("calculator.collapse") : t("calculator.expand")}
                    </button>
                  </div>
                </div>
                {expanded ? (
                  <div className="mt-4 space-y-3 border-t border-[#f2f3f5] pt-4">
                    {calculateResult.repaymentPlan.map((item) => (
                      <div key={item.period} className="flex justify-between items-center">
                        <span className="text-[#86909c] text-[13px]">{t("calculator.periodLabel", { period: item.period })}</span>
                        <div className="text-right">
                          <p className="text-[#86909c] text-[12px]">{formatMonthDay(item.date, locale)}</p>
                          <p className="text-[#1d2129] text-[14px] font-medium">{formatCurrency(item.total, locale)}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : null}
              </>
            ) : error ? (
              <PageError message={error} onAction={() => void loadCalculation(amount, selectedTerm)} />
            ) : null}
          </div>

          <div className="bg-white rounded-2xl border border-[#f2f3f5] px-5 py-4">
            <div className="flex justify-between items-center">
              <span className="text-[#4e5969] text-[15px]">{t("calculator.receivingAccount")}</span>
              <span className="text-[#1d2129] text-[15px] font-medium">{receivingAccountLabel}</span>
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-[#f2f3f5] px-5 py-4">
            <div className="flex justify-between items-center">
              <span className="text-[#4e5969] text-[15px]">{t("calculator.lender")}</span>
              <span className="text-[#1d2129] text-[15px] font-medium">{config?.lender ?? "--"}</span>
            </div>
          </div>

          <div className="bg-[#f7f8fa] rounded-2xl border border-[#e5e6eb] px-5 py-4">
            <div className="flex justify-between items-center mb-1">
              <span className="text-[#86909c] text-[13px]">{t("calculator.annualRate")}</span>
              <span className="text-[#4e5969] text-[13px] font-medium">{annualRate}</span>
            </div>
            <p className="text-[#86909c] text-[12px]">{t("calculator.annualRateTip")}</p>
          </div>

          {error && config ? <PageError message={error} onAction={() => void loadCalculation(amount, selectedTerm)} /> : null}

          <div className="flex items-start gap-3 px-1 py-2">
            <button
              onClick={() => setAgreed((previous) => !previous)}
              className={`mt-0.5 w-[18px] h-[18px] rounded flex-shrink-0 transition-all ${
                agreed ? "bg-[#165dff] border border-[#165dff]" : "bg-white border border-[#c9cdd4]"
              } flex items-center justify-center`}
            >
              {agreed ? (
                <svg width="10" height="8" viewBox="0 0 10 8" fill="none">
                  <path d="M1 4L3.5 6.5L9 1" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              ) : null}
            </button>
            <p className="text-[#4e5969] text-[13px] leading-relaxed">
              {t("calculator.agreementPrefix")}
              {agreements.map((agreement, index) => (
                <span key={agreement}>
                  <span className="text-[#165dff]">{agreement}</span>
                  {index < agreements.length - 1 ? agreementSeparator : ""}
                </span>
              ))}
              {t("calculator.agreementSuffix")}
            </p>
          </div>
        </div>
      </div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 py-4 border-t border-[#f2f3f5]">
        <button
          onClick={() => void handleSubmit()}
          disabled={isSubmitDisabled}
          className={`w-full h-14 rounded-full text-white text-[17px] font-semibold shadow-[0px_8px_24px_rgba(22,93,255,0.35)] transition-opacity ${
            isSubmitDisabled
              ? "bg-[#c9cdd4] shadow-none cursor-not-allowed"
              : "bg-gradient-to-r from-[#165dff] to-[#4d8fff] active:opacity-90"
          }`}
        >
          {isSubmitting ? `${t("calculator.submit")}...` : t("calculator.submit")}
        </button>
      </div>

      <Drawer open={drawerOpen} onOpenChange={setDrawerOpen}>
        <DrawerContent className="mx-auto w-full max-w-[440px] rounded-t-[24px] border-none bg-white">
          <DrawerHeader className="px-5 pt-5 pb-2 text-left">
            <DrawerTitle className="text-[#1d2129] text-[18px]">{t("calculator.editAmount")}</DrawerTitle>
            <DrawerDescription className="text-[#86909c] text-sm">{amountRangeLabel}</DrawerDescription>
          </DrawerHeader>

          <div className="px-5 pb-2">
            <input
              type="number"
              inputMode="numeric"
              min={config?.amountRange.min}
              max={config?.amountRange.max}
              step={drawerStep}
              value={draftAmount}
              onChange={(event) => setDraftAmount(event.target.value)}
              className="w-full h-14 rounded-2xl border border-[#e5e6eb] px-4 text-[28px] font-bold text-[#1d2129] outline-none"
            />
            <div className="grid grid-cols-3 gap-3 mt-4">
              {drawerQuickActions.map((quickAmount) => (
                <button
                  key={quickAmount}
                  onClick={() => setDraftAmount(String(quickAmount))}
                  className="h-11 rounded-xl border border-[#e5e6eb] text-[#1d2129] text-sm font-medium"
                >
                  {formatCurrency(quickAmount, locale)}
                </button>
              ))}
            </div>
          </div>

          <DrawerFooter className="px-5 pb-6 pt-3">
            <button
              onClick={() => {
                if (!config) {
                  return;
                }
                const normalizedAmount = normalizeAmount(Number(draftAmount || amount), config.amountRange);
                setAmount(normalizedAmount);
                setDraftAmount(String(normalizedAmount));
                setDrawerOpen(false);
              }}
              className="w-full h-14 bg-gradient-to-r from-[#165dff] to-[#4d8fff] rounded-full text-white text-[17px] font-semibold"
            >
              {t("calculator.submit")}
            </button>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </MobileLayout>
  );
}

function normalizeAmount(nextAmount: number, amountRange: AmountRange): number {
  if (Number.isNaN(nextAmount)) {
    return amountRange.default;
  }

  const boundedAmount = Math.min(Math.max(nextAmount, amountRange.min), amountRange.max);
  const steps = Math.round((boundedAmount - amountRange.min) / amountRange.step);
  return amountRange.min + (steps * amountRange.step);
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
