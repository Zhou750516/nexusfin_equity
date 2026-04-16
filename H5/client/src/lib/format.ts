import type { Locale } from "@/i18n/locale";

interface FormatCurrencyOptions {
  includeSymbol?: boolean;
}

export function formatCurrency(value: number, locale: Locale, options: FormatCurrencyOptions = {}): string {
  const hasFraction = Math.abs(value % 1) > Number.EPSILON;
  const formatted = new Intl.NumberFormat(locale, {
    minimumFractionDigits: hasFraction ? 2 : 0,
    maximumFractionDigits: 2,
  }).format(value);

  return options.includeSymbol === false ? formatted : `¥${formatted}`;
}

export function formatBankCard(bankName: string, lastFour: string, locale: Locale): string {
  return locale.startsWith("zh") ? `${bankName} ${lastFour}` : `${bankName} (${lastFour})`;
}

export function formatMonthDay(dateValue: string, locale: Locale): string {
  const parsedDate = new Date(dateValue);
  if (Number.isNaN(parsedDate.getTime())) {
    return dateValue;
  }

  return new Intl.DateTimeFormat(locale, {
    month: locale.startsWith("zh") ? "numeric" : "short",
    day: "numeric",
  }).format(parsedDate);
}

export function formatDateTime(dateValue: string, locale: Locale): string {
  const parsedDate = new Date(dateValue);
  if (Number.isNaN(parsedDate.getTime())) {
    return dateValue;
  }

  return new Intl.DateTimeFormat(locale, {
    year: "numeric",
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(parsedDate);
}
