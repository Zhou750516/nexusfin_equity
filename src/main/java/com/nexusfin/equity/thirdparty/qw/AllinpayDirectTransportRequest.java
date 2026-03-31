package com.nexusfin.equity.thirdparty.qw;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

public record AllinpayDirectTransportRequest(
        URI targetUri,
        HttpMethod method,
        MediaType contentType,
        String body,
        Map<String, String> headers,
        Map<String, String> attributes
) {
}
