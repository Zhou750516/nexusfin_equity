package com.nexusfin.equity.service;

import com.nexusfin.equity.enums.H5Locale;

public interface H5I18nService {

    H5Locale currentLocale();

    String text(String key, String fallback);
}
