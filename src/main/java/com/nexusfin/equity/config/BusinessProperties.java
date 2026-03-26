package com.nexusfin.equity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.business")
public class BusinessProperties {

    private String defaultProviderCode = "QW";

    public String getDefaultProviderCode() {
        return defaultProviderCode;
    }

    public void setDefaultProviderCode(String defaultProviderCode) {
        this.defaultProviderCode = defaultProviderCode;
    }
}
