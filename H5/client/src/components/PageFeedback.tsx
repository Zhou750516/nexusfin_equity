import { Skeleton } from "@/components/ui/skeleton";
import { useI18n } from "@/i18n/I18nProvider";
import type { Locale } from "@/i18n/locale";

const FEEDBACK_COPY: Record<Locale, { loading: string; failedTitle: string; retry: string }> = {
  "zh-CN": {
    loading: "加载中...",
    failedTitle: "加载失败",
    retry: "重试",
  },
  "zh-TW": {
    loading: "載入中...",
    failedTitle: "載入失敗",
    retry: "重試",
  },
  "en-US": {
    loading: "Loading...",
    failedTitle: "Failed to load",
    retry: "Retry",
  },
  "vi-VN": {
    loading: "Đang tải...",
    failedTitle: "Tải thất bại",
    retry: "Thử lại",
  },
};

interface PageLoadingProps {
  lines?: number;
}

export function PageLoading({ lines = 4 }: PageLoadingProps) {
  const { locale } = useI18n();
  const copy = FEEDBACK_COPY[locale];

  return (
    <div className="px-4 pt-4 space-y-4">
      <div className="h5-card space-y-3">
        <p className="text-sm text-h5-text-secondary">{copy.loading}</p>
        {Array.from({ length: lines }, (_, index) => (
          <Skeleton key={index} className="h-5 w-full rounded-xl" />
        ))}
      </div>
    </div>
  );
}

interface PageErrorProps {
  message: string;
  onAction?: () => void;
  actionLabel?: string;
}

export function PageError({ message, onAction, actionLabel }: PageErrorProps) {
  const { locale } = useI18n();
  const copy = FEEDBACK_COPY[locale];

  return (
    <div className="px-4 pt-4">
      <div className="h5-card text-center">
        <p className="mb-2 text-base font-semibold text-h5-text-primary">{copy.failedTitle}</p>
        <p className="mb-4 text-sm leading-6 text-h5-text-secondary">{message}</p>
        {onAction ? (
          <button onClick={onAction} className="h5-primary-button min-w-[120px]">
            {actionLabel ?? copy.retry}
          </button>
        ) : null}
      </div>
    </div>
  );
}
