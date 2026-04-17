import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { useI18n } from "@/i18n/I18nProvider";
import { getJointDispatchTips } from "@/i18n/content";
import { getBenefitDispatchContext, resolveBenefitDispatch, type BenefitDispatchContextResult, type BenefitDispatchResolveResult } from "@/lib/benefit-dispatch-api";
import { getQueryParam } from "@/lib/route";
import { resolveDispatchPageState } from "@/pages/joint-dispatch.logic";
import { useEffect, useMemo, useState } from "react";

type DispatchLoadState = "loading" | "ready" | "error";

const STATUS_COPY = {
  "zh-CN": {
    redirect: "当前订单允许继续跳转供应商页面。",
    info: "当前订单先展示说明信息，后续再进入正式分发流程。",
    missing: "缺少权益订单号，暂时无法进入分发流程。",
  },
  "zh-TW": {
    redirect: "當前訂單允許繼續跳轉供應商頁面。",
    info: "當前訂單先展示說明資訊，後續再進入正式分發流程。",
    missing: "缺少權益訂單號，暫時無法進入分發流程。",
  },
  "en-US": {
    redirect: "This order can continue to the supplier page.",
    info: "This order currently stays on the info state before the formal dispatch flow is ready.",
    missing: "Benefit order number is missing, so the dispatch flow cannot start.",
  },
  "vi-VN": {
    redirect: "Đơn hàng hiện có thể tiếp tục chuyển tới trang nhà cung cấp.",
    info: "Đơn hàng hiện tạm hiển thị trạng thái hướng dẫn trước khi quy trình phân phát chính thức sẵn sàng.",
    missing: "Thiếu mã đơn quyền lợi nên chưa thể bắt đầu quy trình phân phát.",
  },
} as const;

export default function JointDispatchPage() {
  const { t, locale } = useI18n();
  const benefitOrderNo = getQueryParam("benefitOrderNo");
  const tips = getJointDispatchTips(locale);
  const [loadState, setLoadState] = useState<DispatchLoadState>("loading");
  const [context, setContext] = useState<BenefitDispatchContextResult | null>(null);
  const [resolveResult, setResolveResult] = useState<BenefitDispatchResolveResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!benefitOrderNo) {
      setLoadState("error");
      setError(STATUS_COPY[locale].missing);
      return;
    }

    void loadDispatchData(benefitOrderNo);
  }, [benefitOrderNo, locale]);

  async function loadDispatchData(currentBenefitOrderNo: string) {
    setLoadState("loading");
    setError(null);
    try {
      const [nextContext, nextResolveResult] = await Promise.all([
        getBenefitDispatchContext(currentBenefitOrderNo),
        resolveBenefitDispatch(currentBenefitOrderNo),
      ]);
      setContext(nextContext);
      setResolveResult(nextResolveResult);
      setLoadState("ready");
    } catch (loadError) {
      setLoadState("error");
      setError(loadError instanceof Error ? loadError.message : "Request failed");
    }
  }

  const dispatchState = useMemo(() => resolveDispatchPageState({
    allowRedirect: resolveResult?.allowRedirect ?? false,
    redirectMode: resolveResult?.redirectMode ?? "INTERMEDIATE",
    supplierUrl: resolveResult?.supplierUrl,
  }), [resolveResult]);

  if (loadState === "loading") {
    return (
      <MobileLayout>
        <PageLoading lines={4} />
      </MobileLayout>
    );
  }

  if (loadState === "error") {
    return (
      <MobileLayout>
        <PageError message={error ?? STATUS_COPY[locale].missing} onAction={() => benefitOrderNo ? void loadDispatchData(benefitOrderNo) : undefined} />
      </MobileLayout>
    );
  }

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-8">
        <div className="bg-gradient-to-b from-[#165dff] to-[#3d7aff] px-5 pt-14 pb-20">
          <h1 className="text-white text-[28px] font-bold mb-2">{t("jointDispatch.title")}</h1>
          <p className="text-white/80 text-[14px] leading-6">{t("jointDispatch.subtitle")}</p>
        </div>

        <div className="px-4 -mt-12 space-y-4">
          <div className="bg-white rounded-2xl border border-[#f2f3f5] p-5 shadow-sm">
            <h2 className="text-[#1d2129] text-[18px] font-semibold mb-2">{t("jointDispatch.orderLabel")}</h2>
            <p className="text-[#4e5969] text-[14px] break-all">{benefitOrderNo ?? t("jointDispatch.orderMissing")}</p>
          </div>

          <div className="bg-white rounded-2xl border border-[#f2f3f5] p-5 shadow-sm">
            <h2 className="text-[#1d2129] text-[18px] font-semibold mb-2">
              {dispatchState.type === "redirect" ? "Dispatch Status" : "Dispatch Mode"}
            </h2>
            <p className="text-[#4e5969] text-[14px] leading-6">
              {dispatchState.type === "redirect" ? STATUS_COPY[locale].redirect : STATUS_COPY[locale].info}
            </p>
            {context ? (
              <p className="text-[#86909c] text-[12px] mt-2">
                scene={context.scene} / orderStatus={context.orderStatus}
              </p>
            ) : null}
            {dispatchState.type === "redirect" ? (
              <a
                href={dispatchState.url}
                target="_blank"
                rel="noreferrer"
                className="mt-4 inline-flex items-center rounded-full bg-[#165dff] px-4 py-2 text-white text-[13px] font-medium"
              >
                Continue to Supplier
              </a>
            ) : null}
          </div>

          <div className="bg-white rounded-2xl border border-[#f2f3f5] p-5 shadow-sm">
            <h2 className="text-[#1d2129] text-[18px] font-semibold mb-3">{t("jointDispatch.nextStepsTitle")}</h2>
            <div className="space-y-3">
              {tips.map((tip, index) => (
                <div key={`${tip}-${index}`} className="flex gap-3">
                  <div className="w-6 h-6 rounded-full bg-[#e8f3ff] text-[#165dff] text-xs font-semibold flex items-center justify-center flex-shrink-0">
                    {index + 1}
                  </div>
                  <p className="text-[#4e5969] text-[14px] leading-6">{tip}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </MobileLayout>
  );
}
