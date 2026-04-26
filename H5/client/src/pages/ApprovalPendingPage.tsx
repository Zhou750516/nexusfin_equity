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
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatCurrency } from "@/lib/format";
import { getApprovalStatus } from "@/lib/loan-api";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath, getQueryParam } from "@/lib/route";
import type { ApprovalStatus } from "@/types/loan.types";
import { Check, ChevronRight, Clock, Sparkles, Star, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useLocation } from "wouter";

const MISSING_CONTEXT_COPY: Record<Locale, string> = {
  "zh-CN": "缺少借款申请编号，请返回首页重新发起申请。",
  "zh-TW": "缺少借款申請編號，請返回首頁重新發起申請。",
  "en-US": "Missing application ID. Please go back to the home page and start again.",
  "vi-VN": "Thiếu mã hồ sơ vay. Vui lòng quay lại trang chủ và bắt đầu lại.",
};

export default function ApprovalPendingPage() {
  const [, navigate] = useLocation();
  const loan = useLoan();
  const { locale, t } = useI18n();
  const [statusData, setStatusData] = useState<ApprovalStatus | null>(null);
  const [loadedLocale, setLoadedLocale] = useState<Locale | null>(null);
  const [loadedApplicationId, setLoadedApplicationId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [benefitDismissed, setBenefitDismissed] = useState(false);
  const [dismissDialogOpen, setDismissDialogOpen] = useState(false);
  const [confirmOrderOpen, setConfirmOrderOpen] = useState(false);
  const [verificationCode, setVerificationCode] = useState("");
  const [codeCountdown, setCodeCountdown] = useState(0);
  const countdownTimerRef = useRef<number | null>(null);
  const [matchingDialogOpen, setMatchingDialogOpen] = useState(false);
  const [matchingCountdown, setMatchingCountdown] = useState(0);

  const applicationId = getQueryParam("applicationId") ?? loan.applicationId;
  useEffect(() => {
    if (applicationId && applicationId !== loan.applicationId) {
      loan.setApplicationId(applicationId);
    }
  }, [applicationId]);

  useEffect(() => {
    if (!applicationId) {
      setIsLoading(false);
      return;
    }

    if (!shouldRequestLocalizedData({
      locale,
      loadedLocale,
      requestKey: applicationId,
      loadedRequestKey: loadedApplicationId,
    })) {
      return;
    }

    void loadStatus(applicationId);
  }, [applicationId, loadedApplicationId, loadedLocale, locale]);

  useEffect(() => {
    if (!applicationId || !statusData || statusData.status === "approved" || statusData.status === "rejected") {
      return;
    }

    const intervalId = window.setInterval(() => {
      void loadStatus(applicationId, false);
    }, 10000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [applicationId, locale, statusData?.status]);

  useEffect(() => {
    if (codeCountdown <= 0) {
      return;
    }
    countdownTimerRef.current = window.setTimeout(() => {
      setCodeCountdown((previous) => previous - 1);
    }, 1000);
    return () => {
      if (countdownTimerRef.current) {
        window.clearTimeout(countdownTimerRef.current);
      }
    };
  }, [codeCountdown]);

  function handleSendCode() {
    if (codeCountdown > 0) {
      return;
    }
    setCodeCountdown(60);
  }

  const isCodeComplete = verificationCode.length === 6;

  useEffect(() => {
    if (!matchingDialogOpen) {
      return;
    }
    if (matchingCountdown <= 0) {
      setMatchingDialogOpen(false);
      setBenefitDismissed(true);
      return;
    }
    const id = window.setTimeout(() => {
      setMatchingCountdown((previous) => previous - 1);
    }, 1000);
    return () => {
      window.clearTimeout(id);
    };
  }, [matchingDialogOpen, matchingCountdown]);

  function handleConfirmPayment() {
    if (!isCodeComplete) {
      return;
    }
    setConfirmOrderOpen(false);
    setVerificationCode("");
    setCodeCountdown(0);
    setMatchingCountdown(30);
    setMatchingDialogOpen(true);
  }

  async function loadStatus(currentApplicationId: string, showLoading = true) {
    if (showLoading) {
      setIsLoading(true);
    }
    setError(null);
    try {
      const nextStatus = await getApprovalStatus(currentApplicationId);
      setStatusData(nextStatus);
      setLoadedLocale(locale);
      setLoadedApplicationId(currentApplicationId);
      loan.setApprovalStatus(nextStatus.status);
      loan.setApplicationId(nextStatus.applicationId);

      if (nextStatus.status === "approved" || nextStatus.status === "rejected") {
        navigate(buildPath("/approval-result", { applicationId: nextStatus.applicationId }));
      }
    } catch (loadError) {
      setError(readErrorMessage(loadError));
    } finally {
      if (showLoading) {
        setIsLoading(false);
      }
    }
  }

  if (!applicationId) {
    return (
      <MobileLayout>
        <PageError
          message={MISSING_CONTEXT_COPY[locale]}
          onAction={() => {
            loan.reset();
            navigate("/calculator");
          }}
          actionLabel={t("repaymentSuccess.backHome")}
        />
      </MobileLayout>
    );
  }

  if (isLoading && !statusData) {
    return (
      <MobileLayout>
        <PageLoading lines={5} />
      </MobileLayout>
    );
  }

  if (error && !statusData) {
    return (
      <MobileLayout>
        <PageError message={error} onAction={() => void loadStatus(applicationId)} />
      </MobileLayout>
    );
  }

  const benefitsCard = statusData?.benefitsCard;
  const shouldShowBenefits = Boolean(benefitsCard?.available) && !benefitDismissed;
  const benefitsPath = applicationId
    ? buildPath("/benefits-card", { applicationId })
    : "/benefits-card";

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-[#f7f8fa] via-[#fcfdfd] to-white pb-32">
        <section
          className="px-5 pt-12 pb-12 flex flex-col items-center"
          style={{ backgroundImage: "linear-gradient(141deg, #165dff 0%, #3d8aff 100%)" }}
        >
          <div className="size-20 bg-white/20 rounded-full flex items-center justify-center mb-7">
            <Clock className="size-10 text-white" strokeWidth={2.5} />
          </div>
          <h1 className="text-white text-[28px] font-bold leading-[42px] tracking-[0.4px] text-center">
            {t("approvalPending.title")}
          </h1>
          <p className="mt-3 text-white/80 text-[15px] leading-[22.5px] tracking-tight text-center">
            {t("approvalPending.subtitle")}
          </p>
        </section>

        {shouldShowBenefits ? (
          <div className="px-5 mt-6">
            <button
              type="button"
              onClick={() => navigate(benefitsPath)}
              className="w-full bg-white border border-[#ffe7d6] rounded-2xl px-5 py-4 flex items-center justify-between shadow-[0_4px_12px_rgba(251,175,25,0.08)] active:opacity-90 transition-opacity"
            >
              <div className="flex items-center gap-3">
                <div
                  className="size-10 rounded-xl flex items-center justify-center shadow-[0_4px_8px_rgba(251,175,25,0.25)]"
                  style={{ backgroundImage: "linear-gradient(135deg, #fbaf19 0%, #ff8f1f 100%)" }}
                >
                  <Sparkles className="size-5 text-white" strokeWidth={2} />
                </div>
                <div className="flex flex-col items-start gap-0.5">
                  <p className="text-[#1d2129] text-[15px] font-semibold tracking-tight">
                    惠选卡
                  </p>
                  <p className="text-[#86909c] text-xs leading-[18px]">
                    精选热门权益 · 享超值立减
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-1 shrink-0">
                <span className="text-[#fbaf19] text-sm font-medium tracking-tight">
                  {t("benefits.viewDetail")}
                </span>
                <ChevronRight className="size-4 text-[#fbaf19]" strokeWidth={2} />
              </div>
            </button>
          </div>
        ) : null}

        {error && statusData ? (
          <div className="px-5 pt-6">
            <PageError message={error} onAction={() => void loadStatus(applicationId)} />
          </div>
        ) : null}
      </div>

      {shouldShowBenefits ? (
        <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 pt-6 pb-6 flex flex-col gap-4">
          <button
            onClick={() => setConfirmOrderOpen(true)}
            className="w-full h-14 rounded-full bg-gradient-to-r from-[#165dff] to-[#3d8aff] text-white text-[17px] font-semibold tracking-tight shadow-[0_20px_25px_rgba(22,93,255,0.4),0_8px_10px_rgba(22,93,255,0.4)] active:opacity-90 transition-opacity"
          >
            {t("approvalPending.activate")}
          </button>
          <button
            onClick={() => setDismissDialogOpen(true)}
            className="w-full text-center text-[#86909c] text-[13px] font-medium tracking-tight"
          >
            {t("approvalPending.skip")}
          </button>
        </div>
      ) : null}

      <Dialog open={dismissDialogOpen} onOpenChange={setDismissDialogOpen}>
        <DialogContent
          showCloseButton={false}
          className="sm:max-w-none w-[calc(100%-66px)] max-w-[376px] rounded-3xl border-none bg-white px-6 py-6 gap-0"
        >
          <div className="flex justify-center">
            <div
              className="size-16 rounded-full flex items-center justify-center"
              style={{ backgroundImage: "linear-gradient(135deg, #fbaf19 0%, #ff8f1f 100%)" }}
            >
              <Star className="size-8 text-white fill-white" strokeWidth={2} />
            </div>
          </div>

          <DialogTitle className="mt-4 text-[#1d2129] text-xl font-bold leading-[30px] tracking-tight text-center">
            {t("approvalPending.dismiss.title")}
          </DialogTitle>

          <DialogDescription className="sr-only">
            {t("approvalPending.dismiss.note")}
          </DialogDescription>

          <div
            className="mt-4 rounded-2xl border border-[#ffe8b8] px-4 py-4 flex flex-col gap-3"
            style={{ backgroundImage: "linear-gradient(155deg, #fff9e6 0%, #fff4d6 100%)" }}
          >
            <DismissBullet>
              <span className="text-[#ff6b00] font-bold">{t("approvalPending.dismiss.bullet1Strong")}</span>
              <span>{t("approvalPending.dismiss.bullet1Rest")}</span>
            </DismissBullet>
            <DismissBullet>
              <span>{t("approvalPending.dismiss.bullet2Prefix")}</span>
              <span className="text-[#ff6b00] font-bold">{t("approvalPending.dismiss.bullet2Strong")}</span>
            </DismissBullet>
            <DismissBullet>
              <span>{t("approvalPending.dismiss.bullet3Prefix")}</span>
              <span className="text-[#165dff] font-bold">{t("approvalPending.dismiss.bullet3Strong")}</span>
            </DismissBullet>
          </div>

          <p className="mt-4 text-[#86909c] text-[13px] leading-[21px] tracking-tight text-center">
            {t("approvalPending.dismiss.note")}
          </p>

          <div className="mt-4 flex flex-col gap-3">
            <button
              type="button"
              onClick={() => setDismissDialogOpen(false)}
              className="w-full h-12 rounded-full text-white text-base font-semibold tracking-tight shadow-[0_10px_15px_rgba(251,175,25,0.3),0_4px_6px_rgba(251,175,25,0.3)]"
              style={{ backgroundImage: "linear-gradient(90deg, #fbaf19 0%, #ff8f1f 100%)" }}
            >
              {t("approvalPending.dismiss.continue")}
            </button>
            <button
              type="button"
              onClick={() => {
                setDismissDialogOpen(false);
                setBenefitDismissed(true);
              }}
              className="w-full h-12 rounded-full bg-[#f7f8fa] text-[#86909c] text-base font-medium tracking-tight"
            >
              {t("approvalPending.dismiss.confirm")}
            </button>
          </div>
        </DialogContent>
      </Dialog>

      <Drawer open={confirmOrderOpen} onOpenChange={setConfirmOrderOpen}>
        <DrawerContent className="mx-auto w-full max-w-[440px] rounded-t-[24px] border-none bg-white p-0">
          <DrawerHeader className="px-6 pt-6 pb-0 flex flex-row items-center justify-between space-y-0">
            <DrawerTitle className="text-[#1d2129] text-[18px] font-semibold tracking-tight">
              {t("approvalPending.confirm.title")}
            </DrawerTitle>
            <button
              type="button"
              onClick={() => setConfirmOrderOpen(false)}
              aria-label={t("common.back")}
              className="size-6 flex items-center justify-center"
            >
              <X className="size-5 text-[#86909c]" strokeWidth={2} />
            </button>
          </DrawerHeader>

          <div className="px-6 pt-6 pb-6 flex flex-col gap-6">
            <div className="bg-[#f7f8fa] rounded-2xl px-5 py-5 flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-[#86909c] text-sm leading-[21px] tracking-tight">
                  {t("approvalPending.confirm.payCard")}
                </span>
                <span className="text-[#1d2129] text-[15px] font-medium tracking-tight">
                  {t("approvalPending.confirm.payCardValue")}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-[#86909c] text-sm leading-[21px] tracking-tight">
                  {t("approvalPending.confirm.amount")}
                </span>
                <span className="text-[#ff6b00] text-xl font-bold leading-[30px] tracking-tight">
                  {benefitsCard ? formatCurrency(benefitsCard.price, locale) : "--"}
                </span>
              </div>
            </div>

            <div className="flex flex-col gap-3">
              <label
                htmlFor="approval-pending-verification-code"
                className="text-[#4e5969] text-sm font-medium leading-[21px] tracking-tight"
              >
                {t("approvalPending.confirm.codeLabel")}
              </label>
              <div className="flex gap-3 items-stretch">
                <input
                  id="approval-pending-verification-code"
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  value={verificationCode}
                  onChange={(event) =>
                    setVerificationCode(event.target.value.replace(/\D/g, ""))
                  }
                  placeholder={t("approvalPending.confirm.codePlaceholder")}
                  className="flex-1 min-w-0 h-12 rounded-[14px] border border-[#e5e6eb] px-4 text-[15px] tracking-tight text-[#1d2129] placeholder:text-[#0a0a0a]/50 outline-none"
                />
                <button
                  type="button"
                  onClick={handleSendCode}
                  disabled={codeCountdown > 0}
                  className={`w-[102px] h-12 rounded-[14px] text-sm font-medium tracking-tight transition-colors ${
                    codeCountdown > 0
                      ? "bg-[#e5e6eb] text-[#86909c]"
                      : "bg-[#165dff] text-white"
                  }`}
                >
                  {codeCountdown > 0
                    ? t("approvalPending.confirm.codeCountdown", { seconds: codeCountdown })
                    : t("approvalPending.confirm.sendCode")}
                </button>
              </div>
            </div>

            <button
              type="button"
              onClick={handleConfirmPayment}
              disabled={!isCodeComplete}
              className={`w-full h-12 rounded-full text-base font-medium tracking-tight transition-colors ${
                isCodeComplete
                  ? "bg-gradient-to-r from-[#165dff] to-[#3d8aff] text-white shadow-[0_8px_16px_rgba(22,93,255,0.25)]"
                  : "bg-[#e5e6eb] text-[#c9cdd4]"
              }`}
            >
              {t("approvalPending.confirm.payButton")}
            </button>
          </div>
        </DrawerContent>
      </Drawer>

      <Dialog open={matchingDialogOpen}>
        <DialogContent
          showCloseButton={false}
          className="sm:max-w-none w-[calc(100%-56px)] max-w-[384px] rounded-3xl border-none bg-white p-0 gap-0 overflow-hidden"
        >
          <div
            aria-hidden="true"
            className="pointer-events-none absolute top-0 left-1/2 -translate-x-1/2 h-0.5 w-20"
            style={{ backgroundImage: "linear-gradient(90deg, transparent 0%, #165dff 50%, transparent 100%)" }}
          />

          <div className="px-6 pt-6 pb-6 flex flex-col items-center">
            <div
              className="size-16 rounded-full flex items-center justify-center shadow-[0_10px_15px_rgba(22,93,255,0.3)]"
              style={{ backgroundImage: "linear-gradient(135deg, #165dff 0%, #3d8aff 100%)" }}
            >
              <Sparkles className="size-8 text-white" strokeWidth={2} />
            </div>

            <div
              className="mt-4 px-3 py-1.5 rounded-full"
              style={{ backgroundImage: "linear-gradient(90deg, #fbaf19 0%, #ff8f1f 100%)" }}
            >
              <p className="text-white text-[11px] font-bold tracking-[0.6px] leading-[16.5px]">
                {t("approvalPending.matching.vipBadge")}
              </p>
            </div>

            <DialogTitle
              className="mt-4 text-[20px] font-bold leading-[30px] tracking-tight text-center bg-clip-text text-transparent"
              style={{ backgroundImage: "linear-gradient(90deg, #165dff 0%, #3d8aff 50%, #165dff 100%)" }}
            >
              {t("approvalPending.matching.title")}
            </DialogTitle>

            <DialogDescription className="mt-2 text-[#4e5969] text-sm leading-[21px] tracking-tight text-center">
              {t("approvalPending.matching.subtitle")}
            </DialogDescription>

            <div
              className="mt-9 size-24 rounded-full flex items-center justify-center shadow-[0_10px_15px_rgba(0,0,0,0.1)]"
              style={{ backgroundImage: "linear-gradient(135deg, #f2f6ff 0%, #fff9e6 100%)" }}
            >
              <p className="text-[#1d2129] text-[32px] font-bold leading-[48px] tracking-tight">
                {matchingCountdown}s
              </p>
            </div>

            <div className="mt-7 flex flex-col gap-1.5 items-center">
              {[
                t("approvalPending.matching.bullet1"),
                t("approvalPending.matching.bullet2"),
                t("approvalPending.matching.bullet3"),
              ].map((label) => (
                <div key={label} className="flex items-center gap-2">
                  <span className="size-1 rounded-full bg-[#165dff]" />
                  <span className="text-[#165dff] text-xs leading-[18px]">{label}</span>
                </div>
              ))}
            </div>
          </div>

          <div
            aria-hidden="true"
            className="pointer-events-none absolute bottom-0 left-1/2 -translate-x-1/2 h-0.5 w-20"
            style={{ backgroundImage: "linear-gradient(90deg, transparent 0%, #fbaf19 50%, transparent 100%)" }}
          />
        </DialogContent>
      </Dialog>
    </MobileLayout>
  );
}

function DismissBullet({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex items-start gap-2">
      <div className="bg-[#fbaf19] rounded-full size-5 flex items-center justify-center shrink-0 mt-0.5">
        <Check className="size-3 text-white" strokeWidth={3} />
      </div>
      <p className="text-[#4e5969] text-sm leading-[22.75px] tracking-tight flex-1">
        {children}
      </p>
    </div>
  );
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
