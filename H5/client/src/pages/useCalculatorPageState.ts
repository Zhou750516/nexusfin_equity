import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatBankCard, formatCurrency } from "@/lib/format";
import { calculateLoan, getCalculatorConfig, applyLoan } from "@/lib/loan-api";
import { toLoanPurposeKey } from "@/lib/loan-purpose";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath } from "@/lib/route";
import type { AmountRange, CalculateResult, CalculatorConfig } from "@/types/loan.types";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "wouter";
import { buildApplyLoanPayload } from "./calculator.logic";
import { LOAN_PURPOSE_KEYS, PROTOCOL_KEYS, type ProtocolKey } from "@/components/calculator/calculatorOptions";

const AGREED_PROTOCOLS = ["user_agreement", "loan_agreement", "privacy_policy"];

export function useCalculatorPageState() {
  const [, navigate] = useLocation();
  const loan = useLoan();
  const { locale, t } = useI18n();
  const [config, setConfig] = useState<CalculatorConfig | null>(null);
  const [calculateResult, setCalculateResult] = useState<CalculateResult | null>(null);
  const [loadedConfigLocale, setLoadedConfigLocale] = useState<Locale | null>(null);
  const [amount, setAmount] = useState(loan.amount);
  const [selectedTerm, setSelectedTerm] = useState(loan.term);
  const [draftAmount, setDraftAmount] = useState(String(loan.amount));
  const [purposeKey, setPurposeKey] = useState<(typeof LOAN_PURPOSE_KEYS)[number]>(
    toLoanPurposeKey(loan.purpose),
  );
  const [expanded, setExpanded] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [purposeDrawerOpen, setPurposeDrawerOpen] = useState(false);
  const [protocolDrawerOpen, setProtocolDrawerOpen] = useState(false);
  const [viewedProtocols, setViewedProtocols] = useState<Set<ProtocolKey>>(() => new Set());
  const [partnersDialogOpen, setPartnersDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isCalculating, setIsCalculating] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const allProtocolsAgreed = viewedProtocols.size === PROTOCOL_KEYS.length;
  const firstRepayment = calculateResult?.repaymentPlan[0] ?? null;
  const annualRateValue =
    calculateResult?.annualRate ?? (config ? `${(config.annualRate * 100).toFixed(1)}%` : "--");
  const receivingAccountLabel = config
    ? formatBankCard(config.receivingAccount.bankName, config.receivingAccount.lastFour, locale)
    : "--";
  const amountRangeLabel = config
    ? `${formatCurrency(config.amountRange.min, locale)} - ${formatCurrency(config.amountRange.max, locale)}`
    : t("calculator.amountRange");
  const drawerStep = config?.amountRange.step ?? 100;
  const isSubmitDisabled = !calculateResult || isSubmitting || isCalculating;

  const drawerQuickActions = useMemo(() => {
    if (!config) {
      return [];
    }

    return [config.amountRange.min, config.amountRange.default, config.amountRange.max];
  }, [config]);

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
      const normalizedTerm =
        supportedTerms.includes(loan.term) ? loan.term : (nextConfig.termOptions[0]?.value ?? 3);

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
      const payload = buildApplyLoanPayload({
        amount,
        term: selectedTerm,
        receivingAccountId: config.receivingAccount.accountId ?? loan.receivingAccountId ?? "",
        agreedProtocols: AGREED_PROTOCOLS,
        purposeKey,
      });
      loan.setPurpose(payload.purpose);

      const result = await applyLoan(payload);

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

  return {
    locale,
    t,
    config,
    calculateResult,
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
    confirmDraftAmount() {
      if (!config) {
        return;
      }
      const normalizedAmount = normalizeAmount(Number(draftAmount || amount), config.amountRange);
      setAmount(normalizedAmount);
      setDraftAmount(String(normalizedAmount));
      setDrawerOpen(false);
    },
    selectPurpose(key: string) {
      setPurposeKey(key as (typeof LOAN_PURPOSE_KEYS)[number]);
      setPurposeDrawerOpen(false);
    },
    viewProtocol(key: string) {
      setViewedProtocols((previous) => {
        const next = new Set(previous);
        next.add(key as ProtocolKey);
        return next;
      });
    },
  };
}

function normalizeAmount(nextAmount: number, amountRange: AmountRange): number {
  if (Number.isNaN(nextAmount)) {
    return amountRange.default;
  }

  const boundedAmount = Math.min(Math.max(nextAmount, amountRange.min), amountRange.max);
  const steps = Math.round((boundedAmount - amountRange.min) / amountRange.step);
  return amountRange.min + steps * amountRange.step;
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
