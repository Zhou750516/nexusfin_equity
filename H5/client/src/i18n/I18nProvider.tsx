import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { setApiLocale } from "@/lib/api";
import { LOCALE_LABELS, messages } from "./messages/index";
import { DEFAULT_LOCALE, type Locale, getInitialLocale, LOCALE_STORAGE_KEY } from "./locale";

interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: string, params?: Record<string, string | number>) => string;
  labels: typeof LOCALE_LABELS;
}

const I18nContext = createContext<I18nContextValue | null>(null);

interface I18nProviderProps {
  children: ReactNode;
}

export function I18nProvider({ children }: I18nProviderProps) {
  const [locale, setLocale] = useState<Locale>(() => {
    const initialLocale = typeof window === "undefined"
      ? DEFAULT_LOCALE
      : getInitialLocale(window.localStorage, window.navigator.languages);

    setApiLocale(initialLocale);
    return initialLocale;
  });
  const handleSetLocale = useCallback((nextLocale: Locale) => {
    setApiLocale(nextLocale);
    setLocale(nextLocale);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") return;
    setApiLocale(locale);
    window.localStorage.setItem(LOCALE_STORAGE_KEY, locale);
    document.documentElement.lang = locale;
  }, [locale]);

  const value = useMemo<I18nContextValue>(() => ({
    locale,
    setLocale: handleSetLocale,
    t: (key, params) => {
      const template = messages[locale][key] ?? messages[DEFAULT_LOCALE][key] ?? key;
      if (!params) return template;
      return Object.entries(params).reduce((result, [paramKey, paramValue]) => {
        return result.replaceAll(`{${paramKey}}`, String(paramValue));
      }, template);
    },
    labels: LOCALE_LABELS,
  }), [handleSetLocale, locale]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useI18n must be used within I18nProvider");
  }
  return context;
}
