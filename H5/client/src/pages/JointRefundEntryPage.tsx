import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { useI18n } from "@/i18n/I18nProvider";
import { getJointRefundNotes } from "@/i18n/content";
import { getRefundInfo, type RefundInfoResult } from "@/lib/refund-api";
import { getQueryParam } from "@/lib/route";
import { resolveRefundPageState } from "@/pages/joint-refund.logic";
import { useEffect, useMemo, useState } from "react";

type RefundLoadState = "loading" | "ready" | "error";

const REFUND_STATUS_COPY = {
  "zh-CN": {
    apply: "当前订单可进入退款申请态。",
    processing: "当前退款申请处理中，请稍后查询结果。",
    blocked: "当前订单暂不可退款。",
    missing: "缺少权益订单号，暂时无法进入退款流程。",
  },
  "zh-TW": {
    apply: "當前訂單可進入退款申請狀態。",
    processing: "當前退款申請處理中，請稍後查詢結果。",
    blocked: "當前訂單暫不可退款。",
    missing: "缺少權益訂單號，暫時無法進入退款流程。",
  },
  "en-US": {
    apply: "This order can enter the refund application state.",
    processing: "The refund is processing. Please check the result later.",
    blocked: "This order is currently not refundable.",
    missing: "Benefit order number is missing, so the refund flow cannot start.",
  },
  "vi-VN": {
    apply: "Đơn hàng hiện có thể vào trạng thái gửi yêu cầu hoàn tiền.",
    processing: "Yêu cầu hoàn tiền đang được xử lý. Vui lòng kiểm tra lại sau.",
    blocked: "Đơn hàng hiện chưa thể hoàn tiền.",
    missing: "Thiếu mã đơn quyền lợi nên chưa thể bắt đầu quy trình hoàn tiền.",
  },
} as const;

export default function JointRefundEntryPage() {
  const { t, locale } = useI18n();
  const benefitOrderNo = getQueryParam("benefitOrderNo");
  const notes = getJointRefundNotes(locale);
  const [loadState, setLoadState] = useState<RefundLoadState>("loading");
  const [refundInfo, setRefundInfo] = useState<RefundInfoResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!benefitOrderNo) {
      setLoadState("error");
      setError(REFUND_STATUS_COPY[locale].missing);
      return;
    }
    void loadRefundInfo(benefitOrderNo);
  }, [benefitOrderNo, locale]);

  async function loadRefundInfo(currentBenefitOrderNo: string) {
    setLoadState("loading");
    setError(null);
    try {
      const nextRefundInfo = await getRefundInfo(currentBenefitOrderNo);
      setRefundInfo(nextRefundInfo);
      setLoadState("ready");
    } catch (loadError) {
      setLoadState("error");
      setError(loadError instanceof Error ? loadError.message : "Request failed");
    }
  }

  const refundState = useMemo(() => resolveRefundPageState({
    refundable: refundInfo?.refundable ?? false,
    refundStatus: refundInfo?.refundStatus,
  }), [refundInfo]);

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
        <PageError message={error ?? REFUND_STATUS_COPY[locale].missing} onAction={() => benefitOrderNo ? void loadRefundInfo(benefitOrderNo) : undefined} />
      </MobileLayout>
    );
  }

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-8">
        <div className="bg-gradient-to-b from-[#ff8a00] to-[#ffb347] px-5 pt-14 pb-20">
          <h1 className="text-white text-[28px] font-bold mb-2">{t("jointRefund.title")}</h1>
          <p className="text-white/85 text-[14px] leading-6">{t("jointRefund.subtitle")}</p>
        </div>

        <div className="px-4 -mt-12 space-y-4">
          <div className="bg-white rounded-2xl border border-[#f2f3f5] p-5 shadow-sm">
            <h2 className="text-[#1d2129] text-[18px] font-semibold mb-2">{t("jointRefund.orderLabel")}</h2>
            <p className="text-[#4e5969] text-[14px] break-all">{benefitOrderNo ?? t("jointRefund.orderMissing")}</p>
          </div>

          <div className="bg-white rounded-2xl border border-[#f2f3f5] p-5 shadow-sm">
            <h2 className="text-[#1d2129] text-[18px] font-semibold mb-2">Refund Status</h2>
            <p className="text-[#4e5969] text-[14px] leading-6">
              {REFUND_STATUS_COPY[locale][refundState.type]}
            </p>
            {refundInfo ? (
              <p className="text-[#86909c] text-[12px] mt-2">
                refundStatus={refundInfo.refundStatus} / refundableAmount={refundInfo.refundableAmount}
              </p>
            ) : null}
          </div>

          <div className="bg-white rounded-2xl border border-[#f2f3f5] p-5 shadow-sm">
            <h2 className="text-[#1d2129] text-[18px] font-semibold mb-3">{t("jointRefund.notesTitle")}</h2>
            <ul className="space-y-3">
              {notes.map((note, index) => (
                <li key={`${note}-${index}`} className="text-[#4e5969] text-[14px] leading-6">
                  {index + 1}. {note}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </MobileLayout>
  );
}
