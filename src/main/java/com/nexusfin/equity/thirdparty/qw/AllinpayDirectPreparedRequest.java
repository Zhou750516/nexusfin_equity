package com.nexusfin.equity.thirdparty.qw;

import java.net.URI;
import org.springframework.http.MediaType;

public record AllinpayDirectPreparedRequest(
        URI targetUri,
        MediaType contentType,
        String requestBody,
        String signature
) {
}
