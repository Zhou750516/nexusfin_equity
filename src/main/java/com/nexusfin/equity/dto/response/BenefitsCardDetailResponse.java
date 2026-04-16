package com.nexusfin.equity.dto.response;

import java.util.List;

public record BenefitsCardDetailResponse(
        String cardName,
        Long price,
        Long totalSaving,
        List<Feature> features,
        List<Category> categories,
        List<String> tips,
        List<ProtocolLink> protocols
) {

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
