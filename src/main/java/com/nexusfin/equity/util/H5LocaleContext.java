package com.nexusfin.equity.util;

import com.nexusfin.equity.enums.H5Locale;

public final class H5LocaleContext {

    private static final ThreadLocal<H5Locale> LOCALE_HOLDER = new ThreadLocal<>();

    private H5LocaleContext() {
    }

    public static void bind(H5Locale locale) {
        LOCALE_HOLDER.set(locale == null ? H5Locale.ZH_CN : locale);
    }

    public static H5Locale current() {
        H5Locale locale = LOCALE_HOLDER.get();
        return locale == null ? H5Locale.ZH_CN : locale;
    }

    public static void clear() {
        LOCALE_HOLDER.remove();
    }
}
