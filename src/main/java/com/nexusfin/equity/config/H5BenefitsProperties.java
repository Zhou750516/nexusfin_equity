package com.nexusfin.equity.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "nexusfin.h5.benefits")
public record H5BenefitsProperties(
        String productCode,
        @DefaultValue("true") boolean protocolLinkRequired,
        @DefaultValue("false") boolean useLocalReceivingAccount,
        @DefaultValue("") String benefitRedirectPublicBaseUrl,
        Activate activate,
        Detail detail
) {

    public record Activate(
            Long defaultLoanAmount,
            Long defaultBenefitAmount,
            String supportedCardType,
            String successMessage
    ) {
    }

    public record Detail(
            String cardName,
            Long price,
            Long totalSaving,
            List<Feature> features,
            List<Category> categories,
            List<String> tips,
            List<ProtocolLink> protocols
    ) {
    }

    public record Feature(
            String title,
            String description
    ) {
    }

    public record Category(
            String name,
            String icon,
            List<Item> benefits
    ) {
    }

    public record Item(
            String discount,
            String title,
            String description,
            String validity,
            Long originalPrice,
            Long saving
    ) {
    }

    public record ProtocolLink(
            String name,
            String url
    ) {
    }
}
