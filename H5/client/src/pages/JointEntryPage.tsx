import MobileLayout from "@/components/MobileLayout";
import { PageError, PageLoading } from "@/components/PageFeedback";
import { useI18n } from "@/i18n/I18nProvider";
import { jointLogin } from "@/lib/auth-api";
import { persistJointLoginParams } from "@/lib/joint-session";
import type { JointLoginParams } from "@/types/loan.types";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "wouter";
import { parseJointLoginParams, resolveJointEntryErrorKey, resolveJointEntryTarget } from "./joint-entry.logic";

type JointEntryStatus = "loading" | "error";

export default function JointEntryPage() {
  const [, navigate] = useLocation();
  const { t } = useI18n();
  const [status, setStatus] = useState<JointEntryStatus>("loading");
  const [error, setError] = useState<string | null>(null);

  const requestPayload = useMemo<JointLoginParams | null>(() => {
    if (typeof window === "undefined") {
      return null;
    }
    return parseJointLoginParams(window.location.search);
  }, []);

  useEffect(() => {
    if (!requestPayload) {
      setStatus("error");
      setError(t("jointEntry.missingParams"));
      return;
    }

    persistJointLoginParams(requestPayload);

    let active = true;
    void (async () => {
      try {
        const result = await jointLogin(requestPayload);
        if (!active) {
          return;
        }
        navigate(resolveJointEntryTarget(result));
      } catch (loadError) {
        if (!active) {
          return;
        }
        setStatus("error");
        if (loadError instanceof Error) {
          setError(t(resolveJointEntryErrorKey(loadError.message)));
          return;
        }
        setError(t("jointEntry.systemBusy"));
      }
    })();

    return () => {
      active = false;
    };
  }, [navigate, requestPayload, t]);

  if (status === "error") {
    return (
      <MobileLayout>
        <PageError
          message={error ?? t("jointEntry.systemBusy")}
          onAction={() => {
            if (requestPayload) {
              window.location.reload();
              return;
            }
            navigate("/calculator");
          }}
          actionLabel={requestPayload ? t("common.retry") : t("repaymentSuccess.backHome")}
        />
      </MobileLayout>
    );
  }

  return (
    <MobileLayout>
      <div className="flex-1 overflow-y-auto bg-[#f7f8fa] pb-8">
        <div className="bg-gradient-to-b from-[#165dff] to-[#3d7aff] px-5 pt-14 pb-20">
          <h1 className="text-white text-[28px] font-bold mb-2">{t("jointEntry.title")}</h1>
          <p className="text-white/80 text-[14px] leading-6">{t("jointEntry.loadingDescription")}</p>
        </div>
        <PageLoading lines={4} />
      </div>
    </MobileLayout>
  );
}
