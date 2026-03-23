package com.nexusfin.equity.dto.response;

public record HealthStatusResponse(
        String service,
        String status,
        String timestamp
) {
}
