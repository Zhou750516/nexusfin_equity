import { useI18n } from "@/i18n/I18nProvider";
import { SUPPORTED_LOCALES, type Locale } from "@/i18n/locale";

export default function LanguageSwitcher() {
  const { locale, setLocale, labels, t } = useI18n();

  return (
    <label className="absolute top-3 right-3 z-20 flex items-center gap-2 rounded-full border border-h5-border-soft bg-white/90 px-3 py-1.5 text-[12px] text-h5-text-secondary shadow-[0_4px_12px_rgba(15,23,42,0.08)] backdrop-blur-sm">
      <span className="text-h5-text-secondary/80">{t("language.label")}</span>
      <select
        value={locale}
        onChange={(event) => setLocale(event.target.value as Locale)}
        className="bg-transparent font-medium text-h5-brand outline-none"
      >
        {SUPPORTED_LOCALES.map((item) => (
          <option key={item} value={item}>
            {labels[item]}
          </option>
        ))}
      </select>
    </label>
  );
}
