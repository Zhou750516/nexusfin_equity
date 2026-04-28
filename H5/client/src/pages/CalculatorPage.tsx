import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
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
import { toLoanPurposeKey } from "@/lib/loan-purpose";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath } from "@/lib/route";
import { buildApplyLoanPayload } from "./calculator.logic";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import type { AmountRange, CalculateResult, CalculatorConfig } from "@/types/loan.types";
import { Check, ChevronRight, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "wouter";

const AGREED_PROTOCOLS = ["user_agreement", "loan_agreement", "privacy_policy"];

const LOAN_PURPOSE_KEYS = [
  "calculator.loanPurpose.shopping",
  "calculator.loanPurpose.rent",
  "calculator.loanPurpose.education",
  "calculator.loanPurpose.travel",
] as const;

const PROTOCOL_KEYS = [
  "calculator.protocol.loanContract",
  "calculator.protocol.privacyAuth",
  "calculator.protocol.userService",
  "calculator.protocol.privacy",
  "calculator.protocol.payment",
] as const;

type ProtocolKey = (typeof PROTOCOL_KEYS)[number];

const PARTNERS = [
  { short: "海尔", full: "海尔消费金融", color: "#3d8aff" },
  { short: "长银", full: "长银消费金融", color: "#ff6b6b" },
  { short: "小米", full: "小米消费金融", color: "#4b93ff" },
  { short: "哈银", full: "哈银消费金融", color: "#ff8c42" },
  { short: "中信", full: "中信消费金融", color: "#e63946" },
  { short: "蒙商", full: "蒙商消金", color: "#3dafff" },
  { short: "本溪", full: "本溪银行", color: "#d62828" },
  { short: "众邦", full: "众邦银行", color: "#165dff" },
];

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

  const firstRepayment = calculateResult?.repaymentPlan[0] ?? null;
  const annualRateValue = calculateResult?.annualRate ?? (config ? `${(config.annualRate * 100).toFixed(1)}%` : "--");
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
      <div className="bg-white/80 backdrop-blur-sm h-[55px] flex items-center px-5 border-b border-[#f2f3f5]">
        <button
          className="text-[#1d2129] text-[15px] font-medium tracking-tight"
          onClick={() => history.back()}
        >
          {t("common.back")}
        </button>
      </div>

      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-[#f7f8fa] via-[#fcfdfd] to-white">
        <div className="px-5 pt-5 pb-6 space-y-4">
          <div
            className="relative rounded-3xl overflow-hidden shadow-[0_20px_25px_-5px_rgba(22,93,255,0.2),0_8px_10px_-6px_rgba(22,93,255,0.2)]"
            style={{ backgroundImage: "linear-gradient(153deg, #165dff 0%, #3d8aff 100%)" }}
          >
            <div className="absolute -left-8 bottom-0 w-24 h-24 bg-white/10 rounded-full blur-[40px]" />
            <div className="relative px-8 py-8">
              <p className="text-white/80 text-sm leading-[21px] tracking-tight">{t("calculator.loanAmount")}</p>
              <div className="mt-3 flex items-end gap-3 border-b border-white pb-3">
                <span className="text-white text-2xl font-medium leading-9 mb-1">¥</span>
                <span className="text-white text-[64px] font-bold leading-[64px] tracking-[0.2px]">
                  {formatCurrency(amount, locale, { includeSymbol: false })}
                </span>
                <button
                  className="ml-auto mb-1 text-white/60 text-[13px] tracking-tight"
                  onClick={() => setDrawerOpen(true)}
                >
                  {t("calculator.editAmount")}
                </button>
              </div>
              <div className="mt-3 flex items-center gap-2">
                <span className="w-1 h-1 bg-white/50 rounded-full" />
                <span className="text-white/70 text-[13px] leading-5 tracking-tight">
                  {amountRangeLabel}
                </span>
              </div>
            </div>
          </div>

          <div className="bg-white border border-[#f2f3f5] rounded-2xl px-5 pt-5 pb-5 shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)]">
            <p className="text-[#1d2129] text-base font-medium tracking-tight">{t("calculator.borrowDuration")}</p>
            <div className="mt-4 flex gap-3">
              {config?.termOptions.map((option) => (
                <button
                  key={option.value}
                  onClick={() => setSelectedTerm(option.value)}
                  className={`flex-1 h-11 rounded-[10px] text-[15px] font-medium tracking-tight transition-all border ${
                    selectedTerm === option.value
                      ? "bg-[#f2f6ff] text-[#165dff] border-[#165dff]"
                      : "bg-white text-[#4e5969] border-[#e5e6eb]"
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          <div className="bg-white border border-[#f2f3f5] rounded-2xl px-5 pt-5 pb-5 shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)]">
            <h3 className="text-[#1d2129] text-[17px] font-semibold tracking-tight">{t("calculator.repaymentTitle")}</h3>
            {isCalculating && !calculateResult ? (
              <div className="mt-3">
                <PageLoading lines={3} />
              </div>
            ) : firstRepayment && calculateResult ? (
              <>
                <div className="flex justify-between items-center pt-4 pb-4 border-b border-[#f2f3f5]">
                  <span className="text-[#4e5969] text-[15px] tracking-tight">{t("calculator.totalFee")}</span>
                  <span className="text-[#ff6b00] text-xl font-bold leading-[30px] tracking-tight">
                    {formatCurrency(calculateResult.totalFee, locale)}
                  </span>
                </div>
                <div className="flex justify-between items-center pt-4">
                  <span className="text-[#4e5969] text-[15px] tracking-tight">{t("calculator.firstRepayment")}</span>
                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <p className="text-[#86909c] text-[13px] leading-5">{formatMonthDay(firstRepayment.date, locale)}</p>
                      <p className="text-[#1d2129] text-[17px] font-bold leading-[25.5px] tracking-tight">
                        {formatCurrency(firstRepayment.total, locale)}
                      </p>
                    </div>
                    <button
                      onClick={() => setExpanded((previous) => !previous)}
                      className="text-[#165dff] text-[13px] font-medium tracking-tight"
                    >
                      {expanded ? t("calculator.collapse") : t("calculator.expand")}
                    </button>
                  </div>
                </div>
                {expanded ? (
                  <div className="mt-4 space-y-3 border-t border-[#f2f3f5] pt-4">
                    {calculateResult.repaymentPlan.map((item) => (
                      <div key={item.period} className="flex justify-between items-center">
                        <span className="text-[#86909c] text-[13px]">
                          {t("calculator.periodLabel", { period: item.period })}
                        </span>
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

          <div className="bg-white border border-[#f2f3f5] rounded-2xl px-5 py-5 shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)]">
            <div className="flex items-center justify-between">
              <span className="text-[#4e5969] text-[15px] tracking-tight">{t("calculator.annualRate")}</span>
              <div className="text-right">
                <p className="text-[#4e5969] text-sm font-medium leading-[21px] tracking-tight">
                  {annualRateValue}
                  {t("calculator.annualRateMethod")}
                </p>
                <p className="mt-1 text-[#86909c] text-xs leading-[18px]">
                  {t("calculator.annualRateTip")}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white border border-[#f2f3f5] rounded-2xl px-5 py-5 shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)]">
            <div className="flex items-center justify-between">
              <span className="text-[#4e5969] text-[15px] tracking-tight">{t("calculator.receivingAccount")}</span>
              <span className="text-[#1d2129] text-[15px] font-medium tracking-tight">{receivingAccountLabel}</span>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setPurposeDrawerOpen(true)}
            className="w-full bg-white border border-[#f2f3f5] rounded-2xl px-5 py-5 shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)] text-left"
          >
            <div className="flex items-center justify-between">
              <span className="text-[#4e5969] text-[15px] tracking-tight">{t("calculator.loanPurpose")}</span>
              <div className="flex items-center gap-2">
                <span className="text-[#1d2129] text-[15px] font-medium tracking-tight">{t(purposeKey)}</span>
                <ChevronRight className="size-4 text-[#86909c]" strokeWidth={2} />
              </div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setProtocolDrawerOpen(true)}
            className="w-full bg-white border border-[#f2f3f5] rounded-2xl px-5 py-5 shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)] text-left"
          >
            <div className="flex items-center justify-between">
              <span className="text-[#4e5969] text-[15px] tracking-tight">{t("calculator.loanProtocol")}</span>
              <span className="text-[#165dff] text-[15px] font-medium tracking-tight">
                {t("calculator.loanProtocolView")}
              </span>
            </div>
          </button>

          <div className="bg-[#f7f8fa] border border-[#e3e4eb] rounded-2xl px-5 py-5">
            <p className="text-[#1d2129] text-[15px] font-medium tracking-tight">{t("calculator.tipsTitle")}</p>
            <div className="mt-4 space-y-3 text-[#4e5969] text-[13px] leading-[21px] tracking-tight">
              <p>
                {t("calculator.tip1Prefix")}
                <button
                  type="button"
                  onClick={() => setPartnersDialogOpen(true)}
                  className="text-[#165dff]"
                >
                  {t("calculator.tip1Highlight")}
                </button>
                {t("calculator.tip1Suffix")}
              </p>
              <p>{t("calculator.tip2")}</p>
              <p>{t("calculator.tip3")}</p>
            </div>
          </div>

          {error && config ? <PageError message={error} onAction={() => void loadCalculation(amount, selectedTerm)} /> : null}

          <button
            onClick={() => void handleSubmit()}
            disabled={isSubmitDisabled}
            className={`mt-2 w-full h-14 rounded-full text-white text-[17px] font-semibold tracking-tight transition-opacity ${
              isSubmitDisabled
                ? "bg-[#c9cdd4] cursor-not-allowed"
                : "bg-gradient-to-r from-[#165dff] to-[#3d8aff] active:opacity-90"
            }`}
          >
            {isSubmitting ? `${t("calculator.submit")}...` : t("calculator.submit")}
          </button>
        </div>
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
              className="w-full h-14 bg-gradient-to-r from-[#165dff] to-[#3d8aff] rounded-full text-white text-[17px] font-semibold"
            >
              {t("calculator.submit")}
            </button>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>

      <Drawer open={purposeDrawerOpen} onOpenChange={setPurposeDrawerOpen}>
        <DrawerContent className="mx-auto w-full max-w-[440px] rounded-t-[24px] border-none bg-white">
          <DrawerHeader className="px-5 py-4 border-b border-[#f2f3f5] flex flex-row items-center justify-between space-y-0">
            <DrawerTitle className="text-[#1d2129] text-[17px] font-semibold tracking-tight">
              {t("calculator.loanPurposeTitle")}
            </DrawerTitle>
            <button
              type="button"
              onClick={() => setPurposeDrawerOpen(false)}
              aria-label={t("common.back")}
              className="size-6 flex items-center justify-center"
            >
              <X className="size-5 text-[#86909c]" strokeWidth={2} />
            </button>
          </DrawerHeader>
          <div className="px-5 pt-2 pb-6 flex flex-col">
            {LOAN_PURPOSE_KEYS.map((key) => {
              const selected = key === purposeKey;
              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => {
                    setPurposeKey(key);
                    setPurposeDrawerOpen(false);
                  }}
                  className={`h-[54px] rounded-[14px] px-4 flex items-center justify-between transition-colors ${
                    selected ? "bg-[#f2f6ff]" : "bg-transparent"
                  }`}
                >
                  <span
                    className={`text-[15px] font-medium tracking-tight ${
                      selected ? "text-[#165dff]" : "text-[#1d2129]"
                    }`}
                  >
                    {t(key)}
                  </span>
                  {selected ? (
                    <Check className="size-5 text-[#165dff]" strokeWidth={2} />
                  ) : null}
                </button>
              );
            })}
          </div>
        </DrawerContent>
      </Drawer>

      <Drawer open={protocolDrawerOpen} onOpenChange={setProtocolDrawerOpen}>
        <DrawerContent className="mx-auto w-full max-w-[440px] rounded-t-[24px] border-none bg-white p-0">
          <DrawerHeader className="px-5 py-4 border-b border-[#f2f3f5] flex flex-row items-center justify-between space-y-0">
            <DrawerTitle className="text-[#1d2129] text-[17px] font-semibold tracking-tight">
              {t("calculator.protocolDrawerTitle")}
            </DrawerTitle>
            <button
              type="button"
              onClick={() => setProtocolDrawerOpen(false)}
              aria-label={t("common.back")}
              className="size-6 flex items-center justify-center"
            >
              <X className="size-5 text-[#86909c]" strokeWidth={2} />
            </button>
          </DrawerHeader>

          <div className="px-5 pt-4 pb-4 flex flex-col gap-4">
            <div className="bg-[#f2f3f5] border border-[#e5e6eb] rounded-[14px] px-4 py-4">
              <p className="text-[#4e5969] text-sm font-semibold leading-[21px] tracking-tight">
                {t("calculator.protocolImportant")}
              </p>
              <p className="mt-2 text-[#4e5969] text-xs leading-[19.5px]">
                {t("calculator.protocolImportantBody")}
              </p>
            </div>

            <div className="flex flex-col gap-3">
              {PROTOCOL_KEYS.map((key) => (
                <button
                  key={key}
                  type="button"
                  onClick={() =>
                    setViewedProtocols((previous) => {
                      const next = new Set(previous);
                      next.add(key);
                      return next;
                    })
                  }
                  className="bg-[#f7f8fa] rounded-[14px] h-[45px] px-4 flex items-center justify-between text-left"
                >
                  <span className="text-[#1d2129] text-sm leading-[21px] tracking-tight">
                    {t(key)}
                  </span>
                  <ChevronRight className="size-4 text-[#86909c]" strokeWidth={2} />
                </button>
              ))}
            </div>
          </div>

          <DrawerFooter className="px-5 pt-4 pb-5 border-t border-[#f2f3f5]">
            <button
              type="button"
              disabled={!allProtocolsAgreed}
              onClick={() => setProtocolDrawerOpen(false)}
              className={`w-full h-12 rounded-full text-base font-medium tracking-tight transition-colors ${
                allProtocolsAgreed
                  ? "bg-gradient-to-r from-[#165dff] to-[#3d8aff] text-white"
                  : "bg-[#e5e6eb] text-[#c9cdd4] cursor-not-allowed"
              }`}
            >
              {t("calculator.protocolAgreeButton", { count: viewedProtocols.size })}
            </button>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>

      <Dialog open={partnersDialogOpen} onOpenChange={setPartnersDialogOpen}>
        <DialogContent
          showCloseButton={false}
          className="sm:max-w-none w-[calc(100%-44px)] max-w-[400px] rounded-3xl border-none bg-white px-6 py-6 gap-0"
        >
          <DialogTitle className="text-[#1d2129] text-[18px] font-semibold leading-[27px] tracking-tight text-center">
            {t("calculator.partnersTitle")}
          </DialogTitle>
          <DialogDescription className="mt-3 text-[#4e5969] text-sm leading-[22.75px] tracking-tight text-center">
            {t("calculator.partnersDescription")}
          </DialogDescription>

          <div className="mt-5 grid grid-cols-2 gap-x-4 gap-y-4">
            {PARTNERS.map((partner) => (
              <div key={partner.full} className="flex items-center gap-3">
                <div
                  className="size-10 rounded-full flex items-center justify-center shrink-0 text-white text-xs leading-[18px]"
                  style={{ backgroundColor: partner.color }}
                >
                  {partner.short}
                </div>
                <span className="text-[#1d2129] text-sm leading-[21px] tracking-tight">
                  {partner.full}
                </span>
              </div>
            ))}
          </div>

          <p className="mt-4 text-[#86909c] text-[13px] leading-[19.5px] tracking-tight text-center">
            {t("calculator.partnersFootnote")}
          </p>

          <button
            type="button"
            onClick={() => setPartnersDialogOpen(false)}
            className="mt-5 w-full h-12 rounded-full bg-[#fbaf19] text-white text-base font-medium leading-6 tracking-tight"
          >
            {t("calculator.partnersAck")}
          </button>
        </DialogContent>
      </Dialog>
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
