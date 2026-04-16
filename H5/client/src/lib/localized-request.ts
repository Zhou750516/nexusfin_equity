import type { Locale } from "@/i18n/locale";

interface LocalizedRequestArgs {
  locale: Locale;
  loadedLocale: Locale | null;
  requestKey?: string | null;
  loadedRequestKey?: string | null;
}

export function shouldRequestLocalizedData({
  locale,
  loadedLocale,
  requestKey,
  loadedRequestKey,
}: LocalizedRequestArgs): boolean {
  if (requestKey !== undefined && !requestKey) {
    return false;
  }

  if (loadedLocale !== locale) {
    return true;
  }

  if (requestKey !== undefined) {
    return loadedRequestKey !== requestKey;
  }

  return false;
}
