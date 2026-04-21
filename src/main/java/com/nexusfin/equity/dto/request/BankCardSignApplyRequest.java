package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BankCardSignApplyRequest(
        @NotBlank String accountNo
) {
}
