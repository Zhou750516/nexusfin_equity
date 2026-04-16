package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentProtocolCallbackRequest(
        @NotBlank String requestId,
        String memberId,
        String externalUserId,
        String providerCode,
        @NotBlank String protocolNo,
        String protocolStatus,
        String signRequestNo,
        String channelCode,
        String signedTs
) {
}
