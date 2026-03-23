package com.nexusfin.equity.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterUserRequest(
        @NotBlank String requestId,
        @NotBlank String channelCode,
        @NotNull @Valid UserInfo userInfo
) {
    public record UserInfo(
            @NotBlank String externalUserId,
            @NotBlank String mobileEncrypted,
            @NotBlank String idCardEncrypted,
            @NotBlank String realNameEncrypted
    ) {
    }
}
