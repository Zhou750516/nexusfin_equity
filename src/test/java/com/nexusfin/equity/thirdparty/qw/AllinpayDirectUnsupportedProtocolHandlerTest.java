package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllinpayDirectUnsupportedProtocolHandlerTest {

    private final AllinpayDirectUnsupportedProtocolHandler handler =
            new AllinpayDirectUnsupportedProtocolHandler();

    @Test
    void shouldThrowForTransportExecution() {
        assertThatThrownBy(() -> handler.execute(new AllinpayDirectTransportRequest(
                URI.create("https://tlt-test.allinpay.com/aipg/ProcessServlet"),
                HttpMethod.POST,
                MediaType.APPLICATION_JSON,
                "{}",
                Map.of(),
                Map.of()
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("transport is not implemented");
    }

    @Test
    void shouldThrowForResponseParsing() {
        assertThatThrownBy(() -> handler.parse(
                AllinpayDirectOperation.MEMBER_SYNC,
                "SYNC001",
                new AllinpayDirectVerifiedResponse(200, "{\"ok\":true}", "sig"),
                QwMemberSyncResponse.class
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("response parsing is not implemented");
    }
}
