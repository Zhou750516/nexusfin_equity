package com.nexusfin.equity.enums;

import java.util.Locale;

public enum H5Locale {
    ZH_CN("zh-CN"),
    ZH_TW("zh-TW"),
    EN_US("en-US"),
    VI_VN("vi-VN");

    private final String languageTag;

    H5Locale(String languageTag) {
        this.languageTag = languageTag;
    }

    public String languageTag() {
        return languageTag;
    }

    public static H5Locale resolve(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return ZH_CN;
        }
        String[] tokens = acceptLanguage.split(",");
        for (String token : tokens) {
            H5Locale locale = resolveSingle(token);
            if (locale != null) {
                return locale;
            }
        }
        return ZH_CN;
    }

    private static H5Locale resolveSingle(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.split(";")[0].trim().replace('_', '-').toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "zh", "zh-cn", "zh-hans" -> ZH_CN;
            case "zh-tw", "zh-hk", "zh-hant" -> ZH_TW;
            case "en", "en-us", "en-gb" -> EN_US;
            case "vi", "vi-vn" -> VI_VN;
            default -> null;
        };
    }
}
