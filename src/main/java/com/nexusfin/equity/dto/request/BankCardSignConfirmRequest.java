package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BankCardSignConfirmRequest(
        @NotBlank String accountNo,
        @NotBlank String verificationCode
) {
}
