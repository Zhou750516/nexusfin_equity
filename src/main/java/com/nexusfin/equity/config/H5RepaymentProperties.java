package com.nexusfin.equity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.h5.repayment")
public record H5RepaymentProperties(
        boolean smsRequired
) {
}
