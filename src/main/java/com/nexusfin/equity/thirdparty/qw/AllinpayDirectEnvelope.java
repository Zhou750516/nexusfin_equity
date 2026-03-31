package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

public record AllinpayDirectEnvelope(
        AllinpayDirectOperation operation,
        URI targetUri,
        AllinpayDirectEnvelopeHead head,
        JsonNode businessPayload
) {
}
