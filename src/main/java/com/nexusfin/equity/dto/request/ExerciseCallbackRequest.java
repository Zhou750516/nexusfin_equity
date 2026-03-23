package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExerciseCallbackRequest(
        @NotBlank String requestId,
        @NotBlank String benefitOrderNo,
        @NotBlank String exerciseStatus,
        String exerciseTime,
        String exerciseDetail
) {
}
