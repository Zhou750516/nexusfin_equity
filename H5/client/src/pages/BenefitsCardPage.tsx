import {
  BackArrow,
  CarIcon,
  CardIcon,
  CheckBlueIcon,
  LifeIcon,
  ShopIcon,
  TvIcon,
} from "@/components/Icons";
import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { toast } from "sonner";
import { useLoan } from "@/contexts/LoanContext";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";
import { formatCurrency } from "@/lib/format";
import { activateBenefitsCard, getBenefitsCardDetail } from "@/lib/loan-api";
import { shouldRequestLocalizedData } from "@/lib/localized-request";
import { buildPath, getQueryParam } from "@/lib/route";
import type { BenefitCategory, BenefitsCardDetail } from "@/types/loan.types";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "wouter";

const MISSING_CONTEXT_COPY: Record<Locale, string> = {
  "zh-CN": "缺少借款申请编号，暂无法开通权益。",
  "zh-TW": "缺少借款申請編號，暫時無法開通權益。",
  "en-US": "Missing application ID. Benefits cannot be activated right now.",
  "vi-VN": "Thiếu mã hồ sơ vay nên chưa thể kích hoạt quyền lợi.",
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
  const [error, setError] = useState<string | null>(null);

  const applicationId = getQueryParam("applicationId") ?? loan.applicationId;
  const pendingPath = applicationId ? buildPath("/approval-pending", { applicationId }) : "/approval-pending";

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

  async function handleActivate() {
    if (!applicationId) {
      setError(MISSING_CONTEXT_COPY[locale]);
      return;
    }

    setIsActivating(true);
    setError(null);
    try {
      const response = await activateBenefitsCard({
        applicationId,
        cardType: "huixuan_card",
      });
      loan.setBenefitsCardActivated(response.status === "activated");
      toast.success(response.message);
      navigate(pendingPath);
    } catch (activateError) {
      setError(readErrorMessage(activateError));
    } finally {
      setIsActivating(false);
    }
  }

  const activeCategory = detail?.categories[activeTab] ?? null;
  const activateLabelLines = t("benefits.activateCta").split("\n");

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

  return (
    <MobileLayout>
      <div className="sticky top-0 z-10 bg-white border-b border-gray-100 h-[54px] flex items-center px-5">
        <button onClick={() => navigate(pendingPath)} className="absolute left-5">
          <BackArrow className="w-6 h-6" color="#1d2129" />
        </button>
        <h1 className="w-full text-center text-[#1d2129] text-[17px] font-semibold">{t("benefits.title")}</h1>
      </div>

      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-24">
        <div className="px-4 pt-4 space-y-4">
          {detail ? (
            <>
              <div className="bg-gradient-to-br from-[#165dff] via-[#1a65ff] to-[#4d8fff] rounded-2xl p-5 overflow-hidden relative shadow-[0px_8px_24px_rgba(22,93,255,0.25)]">
                <div className="absolute w-32 h-32 right-[-10px] top-[-10px] bg-white/10 rounded-full blur-3xl" />
                <div className="relative">
                  <div className="flex items-center gap-2 mb-1">
                    <div className="w-8 h-8 rounded-[10px] bg-white/20 flex items-center justify-center">
                      <CardIcon className="w-5 h-5" />
                    </div>
                    <h2 className="text-white text-xl font-bold">{detail.cardName}</h2>
                    <span className="bg-white/20 text-white text-[11px] px-2 py-0.5 rounded-full">{t("benefits.cardTag")}</span>
                  </div>
                  <p className="text-white/70 text-[13px] mb-4">{t("benefits.cardSubtitle")}</p>
                  <div className="bg-white/15 rounded-xl p-4 flex items-center justify-between gap-3">
                    <div>
                      <p className="text-white/70 text-xs mb-1">{t("benefits.priceLabel")}</p>
                      <div className="flex items-baseline gap-0.5">
                        <span className="text-[#ffd700] text-[32px] font-bold leading-none">{formatCurrency(detail.price, locale)}</span>
                      </div>
                    </div>
                    <button
                      onClick={() => void handleActivate()}
                      disabled={isActivating || !applicationId}
                      className={`rounded-xl px-5 py-3 text-white text-[13px] font-semibold shadow-lg whitespace-pre-line ${
                        isActivating || !applicationId
                          ? "bg-[#c9cdd4] cursor-not-allowed shadow-none"
                          : "bg-gradient-to-br from-[#fbaf19] to-[#ff9500]"
                      }`}
                    >
                      {isActivating ? activateLabelLines[0] : activateLabelLines.join("\n")}
                    </button>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-2xl shadow-sm border border-[#f2f3f5] p-5">
                <h3 className="text-[#1d2129] text-[15px] font-semibold mb-4">{t("benefits.featuresTitle")}</h3>
                <div className="space-y-4">
                  {detail.features.map((item, index) => (
                    <div key={`${item.title}-${index}`} className="flex gap-3">
                      <div className="w-5 h-5 mt-0.5 flex-shrink-0 bg-[#165dff]/10 rounded-full flex items-center justify-center">
                        <CheckBlueIcon className="w-3 h-3" />
                      </div>
                      <div>
                        <p className="text-[#1d2129] text-[13px] font-semibold mb-0.5">{item.title}</p>
                        <p className="text-[#86909c] text-[12px] leading-relaxed">{item.description}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-white rounded-2xl shadow-sm border border-[#f2f3f5] p-5">
                <div className="flex justify-between items-center mb-4">
                  <h3 className="text-[#1d2129] text-[15px] font-semibold">{t("benefits.rightsTitle")}</h3>
                  <div className="flex items-baseline gap-1">
                    <span className="text-[#86909c] text-xs">{t("benefits.savePrefix")}</span>
                    <span className="text-[#ff6b00] text-lg font-bold">{formatCurrency(detail.totalSaving, locale)}</span>
                  </div>
                </div>

                <div className="flex gap-0 mb-4 border-b border-[#f2f3f5] overflow-x-auto">
                  {detail.categories.map((category, index) => {
                    const Icon = iconByCategory(category);
                    return (
                      <button
                        key={`${category.name}-${index}`}
                        onClick={() => setActiveTab(index)}
                        className={`flex-1 min-w-[72px] flex flex-col items-center gap-1.5 pb-3 pt-1 transition-all ${
                          activeTab === index ? "border-b-2 border-[#165dff]" : ""
                        }`}
                      >
                        <Icon className="w-6 h-6" active={activeTab === index} />
                        <span className={`text-[11px] ${activeTab === index ? "text-[#165dff] font-semibold" : "text-[#86909c]"}`}>
                          {category.name}
                        </span>
                      </button>
                    );
                  })}
                </div>

                <div className="space-y-3">
                  {activeCategory?.benefits.map((benefit, index) => (
                    <div key={`${benefit.title}-${index}`} className="flex gap-3 bg-[#f7f8fa] rounded-xl p-3">
                      <div className="w-12 h-12 bg-[#fff3e0] rounded-lg flex flex-col items-center justify-center flex-shrink-0">
                        <span className="text-[#ff6b00] text-[16px] font-bold leading-none">{benefit.discount}</span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between mb-1 gap-2">
                          <p className="text-[#1d2129] text-[13px] font-semibold">{benefit.title}</p>
                          <div className="flex items-center gap-1 flex-shrink-0">
                            <span className="text-[#c9cdd4] text-[11px] line-through">{formatCurrency(benefit.originalPrice, locale)}</span>
                            <span className="bg-[#ff6b00]/10 text-[#ff6b00] text-[10px] font-medium px-1.5 py-0.5 rounded">
                              {formatCurrency(benefit.saving, locale)}
                            </span>
                          </div>
                        </div>
                        <p className="text-[#86909c] text-[11px] leading-relaxed whitespace-pre-line">{benefit.description}</p>
                        <p className="text-[#86909c] text-[11px] mt-2">{benefit.validity}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-white rounded-2xl shadow-sm border border-[#f2f3f5] p-5">
                <h3 className="text-[#1d2129] text-[15px] font-semibold mb-3">{t("benefits.tipTitle")}</h3>
                <div className="space-y-2">
                  {detail.tips.map((tip, index) => (
                    <div key={`${tip}-${index}`} className="flex gap-2">
                      <span className="text-[#86909c] text-xs mt-0.5 flex-shrink-0">•</span>
                      <p className="text-[#86909c] text-[12px] leading-relaxed">{tip}</p>
                    </div>
                  ))}
                </div>
                {detail.protocols.length ? (
                  <div className="mt-4 pt-4 border-t border-[#f2f3f5] flex flex-wrap gap-3">
                    {detail.protocols.map((protocol) => (
                      <a key={protocol.url} href={protocol.url} className="text-[#165dff] text-[12px] font-medium">
                        {protocol.name}
                      </a>
                    ))}
                  </div>
                ) : null}
              </div>
            </>
          ) : null}

          {error && detail ? <PageError message={error} onAction={() => void loadDetail()} /> : null}
        </div>
      </div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 py-4 border-t border-[#f2f3f5]">
        <button
          onClick={() => void handleActivate()}
          disabled={isActivating || !applicationId}
          className={`w-full h-14 rounded-full text-white text-[17px] font-semibold transition-opacity ${
            isActivating || !applicationId
              ? "bg-[#c9cdd4] cursor-not-allowed"
              : "bg-gradient-to-r from-[#165dff] to-[#4d8fff] shadow-[0px_8px_24px_rgba(22,93,255,0.35)] active:opacity-90"
          }`}
        >
          {isActivating ? `${t("benefits.openNow")}...` : t("benefits.openNow")}
        </button>
      </div>
    </MobileLayout>
  );
}

function iconByCategory(category: BenefitCategory) {
  switch (category.icon) {
    case "car":
      return CarIcon;
    case "life":
      return LifeIcon;
    case "shop":
      return ShopIcon;
    case "tv":
    default:
      return TvIcon;
  }
}

function readErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed";
}
