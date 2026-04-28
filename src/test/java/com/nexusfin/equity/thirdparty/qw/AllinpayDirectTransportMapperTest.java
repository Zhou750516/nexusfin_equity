package com.nexusfin.equity.thirdparty.qw;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayDirectTransportMapperTest {

    @Test
    void shouldMapPreparedRequestIntoSignatureTransportRequest() {
        AllinpayDirectTransportMapper mapper = new AllinpayDirectSignatureTransportMapper();

        AllinpayDirectTransportRequest transportRequest = mapper.map(new AllinpayDirectPreparedRequest(
                URI.create("https://tlt-test.allinpay.com/aipg/ProcessServlet"),
                MediaType.APPLICATION_JSON,
                "{\"phase\":\"prepared\"}",
                "req-signature"
        ));

        assertThat(transportRequest.targetUri().toString()).isEqualTo("https://tlt-test.allinpay.com/aipg/ProcessServlet");
        assertThat(transportRequest.method()).isEqualTo(HttpMethod.POST);
        assertThat(transportRequest.contentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(transportRequest.body()).isEqualTo("{\"phase\":\"prepared\"}");
        assertThat(transportRequest.headers()).isEmpty();
        assertThat(transportRequest.attributes()).containsEntry("signature", "req-signature");
    }
}
