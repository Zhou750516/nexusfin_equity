package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BankCardSignConfirmRequest(
        @NotNull Long userSignId,
        @NotBlank String verificationCode
) {
}
