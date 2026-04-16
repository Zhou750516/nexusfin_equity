import type { ApprovalStep, StepStatus } from "@/types/loan.types";
import { useI18n } from "@/i18n/I18nProvider";

interface ApprovalStepsCardProps {
  steps: ApprovalStep[];
}

export default function ApprovalStepsCard({ steps }: ApprovalStepsCardProps) {
  const { t } = useI18n();

  return (
    <div className="bg-white rounded-2xl shadow-[0px_4px_20px_rgba(0,0,0,0.08)] px-5 pt-6 pb-5">
      <div className="space-y-0">
        {steps.map((step, index) => {
          const isLast = index === steps.length - 1;
          const styles = getStepStyles(step.status);
          return (
            <div key={`${step.name}-${index}`} className="flex gap-4">
              <div className="flex flex-col items-center">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 border-2 ${styles.iconWrapper}`}>
                  <StepIcon status={step.status} />
                </div>
                {!isLast ? <div className={`w-[2px] flex-1 min-h-[32px] my-1 ${styles.connector}`} /> : null}
              </div>
              <div className={`flex-1 ${!isLast ? "pb-5" : ""}`}>
                <div className="flex justify-between items-center mb-0.5 gap-3">
                  <span className={`text-[15px] font-semibold ${styles.title}`}>{step.name}</span>
                  <span className={`text-[13px] ${styles.badge}`}>{statusLabel(step.status, t)}</span>
                </div>
                <p className={`text-[13px] ${styles.description}`}>{step.description}</p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function statusLabel(status: StepStatus, t: (key: string) => string): string {
  switch (status) {
    case "completed":
      return t("common.completed");
    case "in_progress":
      return t("common.inProgress");
    case "pending":
    default:
      return t("common.pending");
  }
}

function getStepStyles(status: StepStatus) {
  if (status === "completed") {
    return {
      iconWrapper: "bg-[#165dff] border-[#165dff]",
      connector: "bg-[#165dff]/30",
      title: "text-[#1d2129]",
      badge: "text-[#165dff]",
      description: "text-[#86909c]",
    };
  }

  if (status === "in_progress") {
    return {
      iconWrapper: "bg-[#165dff]/70 border-[#165dff]/70",
      connector: "bg-[#e5e6eb]",
      title: "text-[#1d2129]",
      badge: "text-[#165dff]",
      description: "text-[#86909c]",
    };
  }

  return {
    iconWrapper: "bg-[#f2f3f5] border-[#e5e6eb]",
    connector: "bg-[#e5e6eb]",
    title: "text-[#86909c]",
    badge: "text-[#86909c]",
    description: "text-[#c9cdd4]",
  };
}

interface StepIconProps {
  status: StepStatus;
}

function StepIcon({ status }: StepIconProps) {
  if (status === "pending") {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <path d="M15.0006 2.00007H6.00022C5.46977 2.00007 4.96104 2.2108 4.58596 2.58588C4.21087 2.96097 4.00015 3.46969 4.00015 4.00015V20.0007C4.00015 20.5312 4.21087 21.0399 4.58596 21.415C4.96104 21.7901 5.46977 22.0008 6.00022 22.0008H18.0007C18.5311 22.0008 19.0398 21.7901 19.4149 21.415C19.79 21.0399 20.0007 20.5312 20.0007 20.0007V7.00026L15.0006 2.00007Z" stroke="#86909C" strokeWidth="1.99993" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M14 2V6.00014C14 6.5306 14.2107 7.03932 14.5858 7.41441C14.9609 7.7895 15.4696 8.00022 16 8.00022H20.0002" stroke="#86909C" strokeWidth="1.99993" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }

  if (status === "in_progress") {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M12 6V12.0002L16.0002 14.0003" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }

  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
      <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M9 12L11 14L15 10" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
