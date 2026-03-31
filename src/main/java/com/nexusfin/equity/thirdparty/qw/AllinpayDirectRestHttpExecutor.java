package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class AllinpayDirectRestHttpExecutor implements AllinpayDirectHttpExecutor {

    private final RestClient restClient;

    public AllinpayDirectRestHttpExecutor(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public AllinpayDirectRawResponse execute(AllinpayDirectTransportRequest transportRequest) {
        try {
            ResponseEntity<String> response = restClient.method(transportRequest.method())
                    .uri(transportRequest.targetUri())
                    .headers(headers -> transportRequest.headers().forEach(headers::add))
                    .contentType(transportRequest.contentType())
                    .body(transportRequest.body())
                    .retrieve()
                    .toEntity(String.class);
            return new AllinpayDirectRawResponse(
                    response.getStatusCode().value(),
                    response.getBody(),
                    transportRequest.attributes().get("signature"),
                    copyHeaders(response.getHeaders())
            );
        } catch (RestClientException exception) {
            throw new BizException(
                    "ALLINPAY_DIRECT_TRANSPORT_FAILED",
                    "Failed to execute allinpay direct transport request"
            );
        }
    }

    private Map<String, List<String>> copyHeaders(org.springframework.http.HttpHeaders headers) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        headers.forEach((name, values) -> copied.put(name, List.copyOf(values)));
        return copied;
    }
}
