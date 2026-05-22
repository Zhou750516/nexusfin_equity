package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record CreditImageQueryResponse(
        JsonNode payload,
        List<CreditImageSummary> images
) {
}
