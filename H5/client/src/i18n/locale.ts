export const SUPPORTED_LOCALES = ["zh-CN", "zh-TW", "en-US", "vi-VN"] as const;

export type Locale = (typeof SUPPORTED_LOCALES)[number];

export const DEFAULT_LOCALE: Locale = "zh-CN";
export const LOCALE_STORAGE_KEY = "nexusfin.h5.locale";

export function isSupportedLocale(value: string | null | undefined): value is Locale {
  return SUPPORTED_LOCALES.includes(value as Locale);
}

export function normalizeLocale(value: string | null | undefined): Locale {
  if (!value) return DEFAULT_LOCALE;

  const normalized = value.replace("_", "-").toLowerCase();

  if (normalized === "zh" || normalized === "zh-cn" || normalized === "zh-hans") {
    return "zh-CN";
  }
  if (normalized === "zh-tw" || normalized === "zh-hk" || normalized === "zh-hant") {
    return "zh-TW";
  }
  if (normalized === "en" || normalized === "en-us" || normalized === "en-gb") {
    return "en-US";
  }
  if (normalized === "vi" || normalized === "vi-vn") {
    return "vi-VN";
  }

  return DEFAULT_LOCALE;
}

export function detectBrowserLocale(languages: readonly string[] = []): Locale {
  const firstLanguage = languages.find(Boolean);
  return normalizeLocale(firstLanguage);
}

export function getInitialLocale(storage: Pick<Storage, "getItem"> | null, languages: readonly string[]): Locale {
  const storedLocale = storage?.getItem(LOCALE_STORAGE_KEY);
  if (isSupportedLocale(storedLocale)) {
    return storedLocale;
  }
  return detectBrowserLocale(languages);
}
