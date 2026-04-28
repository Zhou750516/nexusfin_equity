import MobileLayout from "@/components/MobileLayout";
import { PageError } from "@/components/PageFeedback";
import { useI18n } from "@/i18n/I18nProvider";
import { useLocation } from "wouter";

export default function JointUnsupportedPage() {
  const { t } = useI18n();
  const [, navigate] = useLocation();

  return (
    <MobileLayout>
      <PageError
        message={t("jointEntry.unsupportedScene")}
        onAction={() => navigate("/calculator")}
        actionLabel={t("repaymentSuccess.backHome")}
      />
    </MobileLayout>
  );
}
