package com.nexusfin.equity.dto.response;

import java.util.List;

public record SignTaskResponse(
        String benefitOrderNo,
        List<SignTaskItem> tasks
) {
    public record SignTaskItem(
            String taskNo,
            String contractType,
            String signStatus,
            String signUrl
    ) {
    }
}
