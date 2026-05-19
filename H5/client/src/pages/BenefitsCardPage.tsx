import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import StickyActionBar from "@/components/shared/StickyActionBar";
import { toast } from "sonner";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatCurrency } from "@/lib/format";
import { readJointLoginParams } from "@/lib/joint-session";
import {
  activateBenefitsCard,
  applyBankCardSign,
  confirmBankCardSign,
  getBankCardSignStatus,
  getBenefitsCardDetail,
} from "@/lib/loan-api";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath, getQueryParam } from "@/lib/route";
import {
  applyBenefitsSign,
  canStartBenefitsActivation,
  checkBenefitsActivationSignGate,
  confirmBenefitsSignAndActivate,
  resolveDefaultBenefitsUserCard,
} from "@/pages/benefits-card.logic";
import type { BenefitCategory, BenefitsCardDetail } from "@/types/loan.types";
import { Car, Check, ChevronLeft, FileText, ShoppingBag, Sparkles, Tv, Utensils, WalletCards, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useLocation } from "wouter";

const MISSING_CONTEXT_COPY: Record<Locale, string> = {
  "zh-CN": "缺少借款申请编号，暂无法开通权益。",
  "zh-TW": "缺少借款申請編號，暫時無法開通權益。",
  "en-US": "Missing application ID. Benefits cannot be activated right now.",
  "vi-VN": "Thiếu mã hồ sơ vay nên chưa thể kích hoạt quyền lợi.",
};

const MISSING_JOINT_TOKEN_COPY: Record<Locale, string> = {
  "zh-CN": "缺少联登凭证，暂无法生成权益跳转链接。",
  "zh-TW": "缺少聯登憑證，暫時無法生成權益跳轉連結。",
  "en-US": "Missing joint-login token. Benefit redirect URL cannot be generated right now.",
  "vi-VN": "Thiếu token đăng nhập liên kết nên chưa thể tạo liên kết quyền lợi.",
};

const PROTOCOL_NOT_READY_COPY: Record<Locale, string> = {
  "zh-CN": "当前扣费协议待完成，点击开通后将先引导您完成齐为签约。",
  "zh-TW": "目前扣費協議待完成，點擊開通後將先引導您完成齊為簽約。",
  "en-US": "Payment signing is pending. Tap activate to complete signing first.",
  "vi-VN": "Chưa hoàn tất ký khấu trừ. Nhấn kích hoạt để hoàn tất ký trước.",
};

const SECTION_COPY: Record<Locale, {
  protocolTitle: string;
  cardTitle: string;
  protocolReady: string;
  protocolPending: string;
  noCard: string;
  viewProtocol: string;
  defaultCard: string;
}> = {
  "zh-CN": {
    protocolTitle: "服务协议",
    cardTitle: "默认扣费卡",
    protocolReady: "签约状态已满足",
    protocolPending: "签约状态待完成",
    noCard: "暂无可用银行卡",
    viewProtocol: "查看",
    defaultCard: "默认",
  },
  "zh-TW": {
    protocolTitle: "服務協議",
    cardTitle: "預設扣費卡",
    protocolReady: "簽約狀態已滿足",
    protocolPending: "簽約狀態待完成",
    noCard: "暫無可用銀行卡",
    viewProtocol: "查看",
    defaultCard: "預設",
  },
  "en-US": {
    protocolTitle: "Agreements",
    cardTitle: "Default Deduction Card",
    protocolReady: "Signing requirements met",
    protocolPending: "Signing still required",
    noCard: "No available bank card",
    viewProtocol: "View",
    defaultCard: "Default",
  },
  "vi-VN": {
    protocolTitle: "Thỏa thuận dịch vụ",
    cardTitle: "Thẻ khấu trừ mặc định",
    protocolReady: "Điều kiện ký kết đã sẵn sàng",
    protocolPending: "Cần hoàn tất ký kết",
    noCard: "Chưa có thẻ ngân hàng khả dụng",
    viewProtocol: "Xem",
    defaultCard: "Mặc định",
  },
};

interface SignDialogState {
  open: boolean;
  accountNo: string;
  bankName: string;
  maskedCardNo: string;
  userSignId: number | null;
  verificationCode: string;
  applySent: boolean;
  isApplying: boolean;
  isConfirming: boolean;
  error: string | null;
}

const CLOSED_SIGN_DIALOG: SignDialogState = {
  open: false,
  accountNo: "",
  bankName: "",
  maskedCardNo: "",
  userSignId: null,
  verificationCode: "",
  applySent: false,
  isApplying: false,
  isConfirming: false,
  error: null,
};

export default function BenefitsCardPage() {
  const [, navigate] = useLocation();
  const loan = useLoan();
  const { locale, t } = useI18n();
  const [detail, setDetail] = useState<BenefitsCardDetail | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [loadedLocale, setLoadedLocale] = useState<Locale | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isActivating, setIsActivating] = useState(false);
  const [isCheckingSign, setIsCheckingSign] = useState(false);
  const [signDialog, setSignDialog] = useState<SignDialogState>(CLOSED_SIGN_DIALOG);
  const [error, setError] = useState<string | null>(null);

  const applicationId = getQueryParam("applicationId") ?? loan.applicationId;
  const pendingPath = applicationId ? buildPath("/approval-pending", { applicationId }) : "/approval-pending";
  const jointLoginToken = readJointLoginParams()?.token ?? null;

  useEffect(() => {
    if (!shouldRequestLocalizedData({ locale, loadedLocale })) {
      return;
    }
    void loadDetail();
  }, [loadedLocale, locale]);

  async function loadDetail() {
    setIsLoading(true);
    setError(null);
    try {
      const nextDetail = await getBenefitsCardDetail();
      setDetail(nextDetail);
      setActiveTab(0);
      setLoadedLocale(locale);
    } catch (loadError) {
      setError(readErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  }

  async function activateCard() {
    if (!applicationId) {
      setError(MISSING_CONTEXT_COPY[locale]);
      return;
    }
    if (!jointLoginToken) {
      setError(MISSING_JOINT_TOKEN_COPY[locale]);
      return;
    }

    setIsActivating(true);
    setError(null);
    try {
      const response = await activateBenefitsCard({
        applicationId,
        cardType: "huixuan_card",
        token: jointLoginToken,
      });
      loan.setBenefitsCardActivated(response.status === "activated");
      toast.success(response.message);
      navigate(pendingPath);
    } catch (activateError) {
      setError(readErrorMessage(activateError));
      throw activateError;
    } finally {
      setIsActivating(false);
    }
  }

  async function handleActivate() {
    if (!applicationId) {
      setError(MISSING_CONTEXT_COPY[locale]);
      return;
    }
    if (!jointLoginToken) {
      setError(MISSING_JOINT_TOKEN_COPY[locale]);
      return;
    }
    if (!detail) {
      return;
    }

    const defaultUserCard = resolveDefaultBenefitsUserCard(detail.userCards);
    setIsCheckingSign(true);
    setError(null);
    try {
      const signGate = await checkBenefitsActivationSignGate({
        userCard: defaultUserCard,
        getSignStatus: getBankCardSignStatus,
      });

      if (signGate.type === "no-card") {
        setError(t("benefits.sign.noCard"));
        return;
      }
      if (signGate.type === "sign-required") {
        setSignDialog({
          ...CLOSED_SIGN_DIALOG,
          open: true,
          accountNo: signGate.accountNo,
          bankName: signGate.bankName,
          maskedCardNo: signGate.maskedCardNo,
        });
        return;
      }
      await activateCard();
    } catch (signCheckError) {
      setError(readErrorMessage(signCheckError));
    } finally {
      setIsCheckingSign(false);
    }
  }

  async function handleSignApply() {
    if (!signDialog.accountNo) {
      setSignDialog((current) => ({ ...current, error: t("benefits.sign.noCard") }));
      return;
    }
    setSignDialog((current) => ({ ...current, isApplying: true, error: null }));
    try {
      const response = await applyBenefitsSign({
        accountNo: signDialog.accountNo,
        applySign: applyBankCardSign,
      });
      setSignDialog((current) => ({
        ...current,
        userSignId: response.userSignId,
        applySent: true,
        isApplying: false,
        error: null,
      }));
      toast.success(t("benefits.sign.smsSent"));
    } catch (applyError) {
      setSignDialog((current) => ({
        ...current,
        isApplying: false,
        error: readErrorMessage(applyError),
      }));
    }
  }

  async function handleSignConfirm() {
    if (signDialog.userSignId == null) {
      setSignDialog((current) => ({ ...current, error: t("benefits.sign.applyFirst") }));
      return;
    }
    if (!signDialog.verificationCode.trim()) {
      setSignDialog((current) => ({ ...current, error: t("benefits.sign.codeRequired") }));
      return;
    }

    setSignDialog((current) => ({ ...current, isConfirming: true, error: null }));
    try {
      await confirmBenefitsSignAndActivate({
        userSignId: signDialog.userSignId,
        verificationCode: signDialog.verificationCode.trim(),
        confirmSign: confirmBankCardSign,
        activate: activateCard,
      });
      toast.success(t("benefits.sign.success"));
      setSignDialog(CLOSED_SIGN_DIALOG);
      void loadDetail();
    } catch (confirmError) {
      setSignDialog((current) => ({
        ...current,
        isConfirming: false,
        error: readErrorMessage(confirmError),
      }));
    }
  }

  function closeSignDialog() {
    if (signDialog.isApplying || signDialog.isConfirming || isActivating) {
      return;
    }
    setSignDialog(CLOSED_SIGN_DIALOG);
  }

  if (isLoading && !detail) {
    return (
      <MobileLayout>
        <PageLoading lines={6} />
      </MobileLayout>
    );
  }

  if (error && !detail) {
    return (
      <MobileLayout>
        <PageError message={error} onAction={() => void loadDetail()} />
      </MobileLayout>
    );
  }

  const activeCategory = detail?.categories[activeTab] ?? null;
  const activateLabelLines = t("benefits.activateCta").split("\n");
  const defaultUserCard = detail ? resolveDefaultBenefitsUserCard(detail.userCards) : null;
  const activationAllowed = detail ? canStartBenefitsActivation({
    applicationId,
    hasJointLoginToken: Boolean(jointLoginToken),
  }) : false;
  const isActivateBusy = isActivating || isCheckingSign;

  return (
    <MobileLayout>
      <div className="sticky top-0 z-20 flex h-[57px] items-center border-b border-h5-border-soft bg-white px-4">
        <button onClick={() => navigate(pendingPath)} className="flex items-center gap-2 text-h5-text-primary">
          <ChevronLeft className="size-5" strokeWidth={2.5} />
          <span className="text-base font-medium tracking-tight">{t("benefits.title")}</span>
        </button>
      </div>

      <div className="h5-page-shell bg-h5-surface pb-32">
        {detail ? (
          <div className="px-5 pt-6 space-y-5">
            <section
              className="relative rounded-2xl overflow-hidden shadow-[0_10px_15px_rgba(0,0,0,0.1),0_4px_6px_rgba(0,0,0,0.1)] px-6 pt-6 pb-6"
              style={{ backgroundImage: "linear-gradient(150deg, #165dff 0%, #0e5ce6 100%)" }}
            >
              <h2 className="text-white text-xl font-bold leading-[30px] tracking-tight">
                {detail.cardName}
              </h2>
              <p className="mt-2 text-white/90 text-sm leading-[21px] tracking-tight">
                {t("benefits.cardSubtitle")}
              </p>
              <div className="mt-5 bg-white rounded-[14px] px-5 pt-5 pb-5 shadow-[0_4px_6px_rgba(0,0,0,0.1),0_2px_4px_rgba(0,0,0,0.1)] flex items-center justify-between">
                <div>
                  <p className="text-[#86909c] text-xs leading-[18px]">
                    {t("benefits.priceLabel")}
                  </p>
                  <p className="mt-1 leading-none">
                    <span className="text-[#fbaf19] text-[28px] font-bold tracking-[0.4px]">
                      {formatCurrency(detail.price, locale, { includeSymbol: false })}
                    </span>
                    <span className="ml-0.5 text-[#fbaf19] text-base font-bold tracking-tight">
                      {t("benefits.priceUnit")}
                    </span>
                  </p>
                </div>
                <button
                  onClick={() => void handleActivate()}
                  disabled={isActivateBusy || !activationAllowed}
                  className="relative h-[64px] min-w-[115px] rounded-full overflow-hidden shadow-[0_8px_24px_-4px_rgba(251,175,25,0.4)] disabled:opacity-60 disabled:cursor-not-allowed"
                  style={{ backgroundImage: "linear-gradient(151deg, #ff6b2c 0%, #fbaf19 50%, #ffd24c 100%)" }}
                >
                  <span className="absolute inset-0 bg-gradient-to-t from-black/10 to-transparent" />
                  <span className="relative flex flex-col items-center justify-center px-4 py-2">
                    <span className="text-white/90 text-xs leading-[18px]">
                      {activateLabelLines[0]}
                    </span>
                    <span className="text-white text-[15px] font-bold leading-[22.5px] tracking-tight">
                      {activateLabelLines[1] ?? ""}
                    </span>
                  </span>
                </button>
              </div>
            </section>

            <section className="bg-white rounded-2xl shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)] px-5 pt-5 pb-5">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <FileText className="size-4 text-[#165dff]" strokeWidth={2.25} />
                  <h3 className="text-[#1d2129] text-base font-bold leading-6 tracking-tight">
                    {SECTION_COPY[locale].protocolTitle}
                  </h3>
                </div>
                <span className={`rounded-full px-3 py-1 text-xs font-medium ${
                  detail.protocolReady
                    ? "bg-[#22c55e]/10 text-[#16a34a]"
                    : "bg-[#fbaf19]/12 text-[#d97706]"
                }`}>
                  {detail.protocolReady ? SECTION_COPY[locale].protocolReady : SECTION_COPY[locale].protocolPending}
                </span>
              </div>
              <div className="mt-4 flex flex-col gap-3">
                {detail.protocols.map((protocol) => (
                  <a
                    key={`${protocol.name}-${protocol.url}`}
                    href={protocol.url}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center justify-between rounded-xl border border-[#e5e6eb] bg-[#f7f8fa] px-4 py-3"
                  >
                    <span className="text-[#1d2129] text-sm font-medium tracking-tight">
                      {protocol.name}
                    </span>
                    <span className="text-[#165dff] text-xs font-semibold tracking-tight">
                      {SECTION_COPY[locale].viewProtocol}
                    </span>
                  </a>
                ))}
              </div>
            </section>

            <section className="bg-white rounded-2xl shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)] px-5 pt-5 pb-5">
              <div className="flex items-center gap-2">
                <WalletCards className="size-4 text-[#165dff]" strokeWidth={2.25} />
                <h3 className="text-[#1d2129] text-base font-bold leading-6 tracking-tight">
                  {SECTION_COPY[locale].cardTitle}
                </h3>
              </div>
              <div className="mt-4 rounded-xl border border-[#e5e6eb] bg-[#f7f8fa] px-4 py-4">
                {defaultUserCard ? (
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-[#1d2129] text-sm font-semibold tracking-tight">
                        {defaultUserCard.bankName}
                      </p>
                      <p className="mt-1 text-[#86909c] text-[13px] tracking-tight">
                        {`**** ${defaultUserCard.cardLastFour}`}
                      </p>
                    </div>
                    <span className="rounded-full bg-[#165dff]/10 px-3 py-1 text-xs font-medium text-[#165dff]">
                      {SECTION_COPY[locale].defaultCard}
                    </span>
                  </div>
                ) : (
                  <p className="text-[#86909c] text-sm tracking-tight">
                    {SECTION_COPY[locale].noCard}
                  </p>
                )}
              </div>
            </section>

            <section className="bg-white rounded-2xl shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)] px-5 pt-5 pb-5">
              <h3 className="text-[#1d2129] text-base font-bold leading-6 tracking-tight">
                {t("benefits.featuresTitle")}
              </h3>
              <div className="mt-4 flex flex-col gap-4">
                {detail.features.map((feature) => (
                  <div key={feature.title} className="flex gap-3 items-start">
                    <div className="size-6 rounded-full bg-[#165dff]/10 flex items-center justify-center shrink-0 mt-0.5">
                      <Check className="size-4 text-[#165dff]" strokeWidth={3} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-[#1d2129] text-sm font-semibold leading-[21px] tracking-tight">
                        {feature.title}
                      </p>
                      <p className="mt-1 text-[#86909c] text-[13px] leading-[21.125px]">
                        {feature.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="bg-white rounded-2xl shadow-[0_1px_3px_rgba(0,0,0,0.1),0_1px_2px_rgba(0,0,0,0.1)] px-5 pt-5 pb-5">
              <div className="flex items-center justify-between">
                <h3 className="text-[#1d2129] text-base font-bold leading-6 tracking-tight">
                  {t("benefits.rightsTitle")}
                </h3>
                <p className="flex items-baseline gap-1">
                  <span className="text-[#86909c] text-[11px]">{t("benefits.savePrefix")}</span>
                  <span
                    className="text-xl font-bold leading-[30px] tracking-tight bg-clip-text text-transparent"
                    style={{ backgroundImage: "linear-gradient(90deg, #ff6b00 0%, #fbaf19 100%)" }}
                  >
                    {formatCurrency(detail.totalSaving, locale, { includeSymbol: false })}
                  </span>
                  <span className="text-[#ff6b00] text-[13px] font-medium">
                    {t("benefits.saveSuffix")}
                  </span>
                </p>
              </div>

              <div className="mt-5 bg-[#f7f8fa] rounded-[10px] p-1 flex gap-1">
                {detail.categories.map((category, index) => {
                  const Icon = iconByCategory(category);
                  const isActive = activeTab === index;
                  return (
                    <button
                      key={`${category.name}-${index}`}
                      onClick={() => setActiveTab(index)}
                      className={`flex-1 h-[58px] rounded-lg flex flex-col items-center justify-center gap-1 ${
                        isActive
                          ? "bg-white shadow-[0_2px_4px_rgba(22,93,255,0.15)]"
                          : "bg-transparent"
                      }`}
                    >
                      <Icon
                        className={`size-[18px] ${isActive ? "text-[#165dff]" : "text-[#86909c]"}`}
                        strokeWidth={2}
                      />
                      <span
                        className={`text-[11px] tracking-tight ${
                          isActive ? "text-[#165dff] font-medium" : "text-[#86909c]"
                        }`}
                      >
                        {category.name}
                      </span>
                    </button>
                  );
                })}
              </div>

              <div className="mt-3 flex flex-col gap-3">
                {activeCategory && activeCategory.benefits.length > 0 ? (
                  activeCategory.benefits.map((benefit) => {
                    const match = benefit.discount.match(/^([\d.]+)(.*)$/);
                    const discountNumber = match?.[1] ?? benefit.discount;
                    const discountSuffix = match?.[2] ?? "";
                    return (
                      <div
                        key={benefit.title}
                        className="relative rounded-[14px] border border-[#ffe7d6] overflow-hidden flex items-stretch h-[104px]"
                        style={{ backgroundImage: "linear-gradient(164deg, #fffbf5 0%, #fff8f0 100%)" }}
                      >
                        <div
                          className="w-20 flex flex-col items-center justify-center gap-1 shrink-0"
                          style={{ backgroundImage: "linear-gradient(128deg, #fff5eb 0%, #ffefe0 100%)" }}
                        >
                          <span className="text-[#ff6b00] text-[32px] font-black leading-8 tracking-[0.4px]">
                            {discountNumber}
                          </span>
                          <span className="text-[#ff6b00] text-[11px] font-medium leading-[16.5px]">
                            {discountSuffix}
                          </span>
                        </div>
                        <div className="border-l border-dashed border-[#e5e6eb]" />
                        <span className="absolute left-[70px] -top-2 size-5 rounded-full bg-[#f7f8fa] border border-[#ffe7d6]" />
                        <span className="absolute left-[70px] -bottom-2 size-5 rounded-full bg-[#f7f8fa] border border-[#ffe7d6]" />
                        <div className="flex-1 min-w-0 px-3 py-3 flex items-center gap-2">
                          <div className="flex-1 min-w-0 flex flex-col gap-1">
                            <p className="text-[#1d2129] text-sm font-bold leading-[17.5px] tracking-tight truncate">
                              {benefit.title}
                            </p>
                            <p className="text-[#4e5969] text-xs leading-[19.5px] line-clamp-2">
                              {benefit.description}
                            </p>
                            <p className="text-[#86909c] text-[10px] leading-[15px]">
                              {benefit.validity}
                            </p>
                          </div>
                          <div className="flex flex-col items-end gap-0.5 shrink-0">
                            <span className="text-[#86909c] text-[10px] leading-[15px] line-through">
                              {formatCurrency(benefit.originalPrice, locale)}
                            </span>
                            <span className="bg-[#00b42a] text-white text-[9px] leading-[13.5px] px-2 py-0.5 rounded-full whitespace-nowrap">
                              {`省${formatCurrency(benefit.saving, locale)}`}
                            </span>
                          </div>
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <p className="py-8 text-center text-[#86909c] text-[13px]">敬请期待</p>
                )}
              </div>
            </section>

            <section className="bg-[#f7f8fa] border border-[#e5e6eb] rounded-[14px] px-4 pt-4 pb-4">
              <h4 className="text-[#4e5969] text-sm font-medium leading-[21px] tracking-tight">
                {t("benefits.tipTitle")}
              </h4>
              <ul className="mt-3 flex flex-col gap-2 pl-1">
                {detail.tips.map((tip) => (
                  <li key={tip} className="flex gap-2">
                    <span className="text-[#86909c] text-xs leading-[19.5px]">•</span>
                    <p className="flex-1 text-[#86909c] text-xs leading-[19.5px]">{tip}</p>
                  </li>
                ))}
              </ul>
            </section>

            {error && detail ? (
              <PageError message={error} onAction={() => void loadDetail()} />
            ) : null}

            {!detail.protocolReady ? (
              <section className="bg-[#fff7e6] border border-[#ffe4b3] rounded-[14px] px-4 py-4">
                <p className="text-[#ad6800] text-[13px] leading-[21px]">
                  {PROTOCOL_NOT_READY_COPY[locale]}
                </p>
              </section>
            ) : null}

            {detail.protocolReady && !jointLoginToken ? (
              <section className="bg-[#fff7e6] border border-[#ffe4b3] rounded-[14px] px-4 py-4">
                <p className="text-[#ad6800] text-[13px] leading-[21px]">
                  {MISSING_JOINT_TOKEN_COPY[locale]}
                </p>
              </section>
            ) : null}
          </div>
        ) : null}
      </div>

      <StickyActionBar
        primary={
          <button
            onClick={() => void handleActivate()}
            disabled={isActivateBusy || !activationAllowed}
            className={`w-full text-base tracking-tight ${
              isActivateBusy || !activationAllowed
                ? "h-14 cursor-not-allowed rounded-full bg-[#c9cdd4] font-semibold text-white"
                : "h5-primary-button w-full text-base"
            }`}
          >
            {isActivateBusy ? `${t("benefits.openNow")}...` : t("benefits.openNow")}
          </button>
        }
      />
      {signDialog.open ? (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/45 px-4 pb-4">
          <div className="w-full max-w-[420px] rounded-[24px] bg-white px-5 pt-5 pb-6 shadow-[0_20px_60px_rgba(15,23,42,0.24)]">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="text-[#1d2129] text-lg font-bold leading-7 tracking-tight">
                  {t("benefits.sign.title")}
                </h3>
                <p className="mt-1 text-[#86909c] text-sm leading-[21px]">
                  {t("benefits.sign.subtitle")}
                </p>
              </div>
              <button
                type="button"
                onClick={closeSignDialog}
                disabled={signDialog.isApplying || signDialog.isConfirming || isActivating}
                className="size-9 rounded-full bg-[#f2f3f5] text-[#4e5969] flex items-center justify-center disabled:opacity-50"
                aria-label={t("benefits.sign.close")}
              >
                <X className="size-4" />
              </button>
            </div>

            <div className="mt-5 rounded-2xl border border-[#e5e6eb] bg-[#f7f8fa] px-4 py-4">
              <p className="text-[#86909c] text-xs leading-[18px]">
                {t("benefits.sign.cardLabel")}
              </p>
              <div className="mt-2 flex items-center justify-between gap-3">
                <span className="text-[#1d2129] text-sm font-semibold tracking-tight">
                  {signDialog.bankName}
                </span>
                <span className="text-[#4e5969] text-sm font-medium tracking-tight">
                  {signDialog.maskedCardNo}
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => void handleSignApply()}
              disabled={signDialog.isApplying || signDialog.isConfirming}
              className="mt-5 h-12 w-full rounded-full bg-[#165dff] text-white text-sm font-semibold tracking-tight disabled:cursor-not-allowed disabled:bg-[#c9cdd4]"
            >
              {signDialog.isApplying
                ? `${t("benefits.sign.getCode")}...`
                : signDialog.applySent
                  ? t("benefits.sign.resendCode")
                  : t("benefits.sign.getCode")}
            </button>

            {signDialog.applySent ? (
              <div className="mt-4">
                <label className="text-[#4e5969] text-sm font-medium" htmlFor="benefits-sign-code">
                  {t("benefits.sign.codeLabel")}
                </label>
                <input
                  id="benefits-sign-code"
                  value={signDialog.verificationCode}
                  onChange={(event) => setSignDialog((current) => ({
                    ...current,
                    verificationCode: event.target.value,
                    error: null,
                  }))}
                  inputMode="numeric"
                  maxLength={8}
                  placeholder={t("benefits.sign.codePlaceholder")}
                  className="mt-2 h-12 w-full rounded-xl border border-[#e5e6eb] bg-white px-4 text-base tracking-[0.2em] outline-none focus:border-[#165dff]"
                />
                <button
                  type="button"
                  onClick={() => void handleSignConfirm()}
                  disabled={signDialog.isConfirming || isActivating}
                  className="mt-4 h-12 w-full rounded-full bg-[#ff6b2c] text-white text-sm font-semibold tracking-tight disabled:cursor-not-allowed disabled:bg-[#c9cdd4]"
                >
                  {signDialog.isConfirming || isActivating
                    ? `${t("benefits.sign.confirm")}...`
                    : t("benefits.sign.confirm")}
                </button>
              </div>
            ) : null}

            {signDialog.error ? (
              <p className="mt-4 rounded-xl bg-[#fff2f0] px-4 py-3 text-[#cf1322] text-[13px] leading-[20px]">
                {signDialog.error}
              </p>
            ) : null}
          </div>
        </div>
      ) : null}
    </MobileLayout>
  );
}

function iconByCategory(category: BenefitCategory) {
  switch (category.icon) {
    case "car":
      return Car;
    case "life":
      return Utensils;
    case "shop":
      return ShoppingBag;
    case "tv":
      return Tv;
    default:
      return Sparkles;
  }
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
