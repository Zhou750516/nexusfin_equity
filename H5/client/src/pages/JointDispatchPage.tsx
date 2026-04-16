import MobileLayout from "@/components/MobileLayout";
import { useI18n } from "@/i18n/I18nProvider";
import { getJointDispatchTips } from "@/i18n/content";
import { getQueryParam } from "@/lib/route";

export default function JointDispatchPage() {
  const { t, locale } = useI18n();
  const benefitOrderNo = getQueryParam("benefitOrderNo");
  const tips = getJointDispatchTips(locale);

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
