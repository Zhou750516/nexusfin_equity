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
import { Car, Check, ChevronLeft, ShoppingBag, Sparkles, Tv, Utensils } from "lucide-react";
import { useEffect, useState } from "react";
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

  return (
    <MobileLayout>
      <div className="sticky top-0 z-20 bg-white border-b border-[#e5e6eb] h-[57px] flex items-center px-4">
        <button onClick={() => navigate(pendingPath)} className="flex items-center gap-2 text-[#1d2129]">
          <ChevronLeft className="size-5" strokeWidth={2.5} />
          <span className="text-base font-medium tracking-tight">{t("benefits.title")}</span>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-32">
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
                      {detail.price}
                    </span>
                    <span className="ml-0.5 text-[#fbaf19] text-base font-bold tracking-tight">
                      {t("benefits.priceUnit")}
                    </span>
                  </p>
                </div>
                <button
                  onClick={() => void handleActivate()}
                  disabled={isActivating || !applicationId}
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
                    {detail.totalSaving}
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
          </div>
        ) : null}
      </div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[440px] bg-white/95 backdrop-blur-sm px-5 py-4 border-t border-[#f2f3f5]">
        <button
          onClick={() => void handleActivate()}
          disabled={isActivating || !applicationId}
          className={`w-full h-14 rounded-full text-white text-base font-semibold tracking-tight transition-opacity ${
            isActivating || !applicationId
              ? "bg-[#c9cdd4] cursor-not-allowed"
              : "bg-[#165dff] shadow-[0_10px_15px_rgba(0,0,0,0.1),0_4px_6px_rgba(0,0,0,0.1)] active:opacity-90"
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
