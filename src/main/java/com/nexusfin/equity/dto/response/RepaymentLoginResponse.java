package com.nexusfin.equity.dto.response;

public record RepaymentLoginResponse(
        boolean authenticated,
        Integer loanId
) {
}
