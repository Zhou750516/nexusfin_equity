package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllinpayDirectSkeletonHttpExecutorTest {

    @Test
    void shouldReportProtocolUnimplementedWhenExecutingPreparedRequest() {
        AllinpayDirectHttpExecutor executor = new AllinpayDirectSkeletonHttpExecutor();

        assertThatThrownBy(() -> executor.execute(new AllinpayDirectTransportRequest(
                URI.create("https://tlt-test.allinpay.com/aipg/ProcessServlet"),
                HttpMethod.POST,
                MediaType.APPLICATION_JSON,
                "{\"phase\":\"prepared\"}",
                java.util.Map.of(),
                java.util.Map.of("signature", "req-signature")
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED")
                .hasMessageContaining("https://tlt-test.allinpay.com/aipg/ProcessServlet");
    }
}
