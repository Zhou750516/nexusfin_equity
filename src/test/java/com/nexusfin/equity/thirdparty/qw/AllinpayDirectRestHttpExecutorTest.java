package com.nexusfin.equity.thirdparty.qw;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AllinpayDirectRestHttpExecutorTest {

    @Test
    void shouldExecuteTransportRequestViaRestClientAndCaptureHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        URI uri = URI.create("https://tlt-test.allinpay.com/aipg/ProcessServlet");
        server.expect(requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Signature", "req-signature"))
                .andExpect(content().json("{\"phase\":\"transport\"}"))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON)
                        .header("X-Resp-Signature", "resp-signature"));
        AllinpayDirectHttpExecutor executor = new AllinpayDirectRestHttpExecutor(builder.build());

        AllinpayDirectRawResponse response = executor.execute(new AllinpayDirectTransportRequest(
                uri,
                HttpMethod.POST,
                MediaType.APPLICATION_JSON,
                "{\"phase\":\"transport\"}",
                Map.of("X-Signature", "req-signature"),
                Map.of("signature", "req-signature")
        ));

        assertThat(response.httpStatus()).isEqualTo(200);
        assertThat(response.responseBody()).isEqualTo("{\"status\":\"ok\"}");
        assertThat(response.headers()).containsEntry("X-Resp-Signature", List.of("resp-signature"));
        server.verify();
    }
}
