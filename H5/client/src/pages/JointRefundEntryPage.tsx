import MobileLayout from "@/components/MobileLayout";
import { useI18n } from "@/i18n/I18nProvider";
import { getJointRefundNotes } from "@/i18n/content";
import { getQueryParam } from "@/lib/route";

export default function JointRefundEntryPage() {
  const { t, locale } = useI18n();
  const benefitOrderNo = getQueryParam("benefitOrderNo");
  const notes = getJointRefundNotes(locale);

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
