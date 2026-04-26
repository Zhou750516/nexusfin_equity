package com.nexusfin.equity.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LoanApplyRequest(
        @NotNull @Min(1) Long amount,
        @NotNull @Min(1) Integer term,
        @NotBlank String receivingAccountId,
        @NotEmpty List<String> agreedProtocols,
        String loanReason,
        String bankCardNum,
        JsonNode basicInfo,
        JsonNode idInfo,
        JsonNode contactInfo,
        JsonNode supplementInfo,
        JsonNode optionInfo,
        JsonNode imageInfo,
        String platformBenefitOrderNo
) {
}
