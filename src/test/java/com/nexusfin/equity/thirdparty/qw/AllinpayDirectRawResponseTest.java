package com.nexusfin.equity.thirdparty.qw;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayDirectRawResponseTest {

    @Test
    void shouldDefaultHeadersToEmptyWhenUsingShortConstructor() {
        AllinpayDirectRawResponse response = new AllinpayDirectRawResponse(200, "{\"ok\":true}", "resp-signature");

        assertThat(response.httpStatus()).isEqualTo(200);
        assertThat(response.responseBody()).isEqualTo("{\"ok\":true}");
        assertThat(response.signature()).isEqualTo("resp-signature");
        assertThat(response.headers()).isEmpty();
    }

    @Test
    void shouldKeepHeadersWhenUsingFullConstructor() {
        AllinpayDirectRawResponse response = new AllinpayDirectRawResponse(
                200,
                "{\"ok\":true}",
                "resp-signature",
                Map.of("X-Resp-Signature", List.of("resp-signature"))
        );

        assertThat(response.headers()).containsKey("X-Resp-Signature");
    }
}
