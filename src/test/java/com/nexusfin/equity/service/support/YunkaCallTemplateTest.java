package com.nexusfin.equity.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YunkaCallTemplateTest {

    @Mock
    private YunkaGatewayClient yunkaGatewayClient;

    @Test
    void shouldExecuteStrictDataCallAndForwardGatewayRequest() {
        YunkaCallTemplate template = new YunkaCallTemplate(yunkaGatewayClient);
        JsonNode data = JsonNodeFactory.instance.objectNode().put("loanId", "LN-001");
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", data));

        JsonNode response = template.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan query",
                        "LQ-001",
                        "/loan/query",
                        "APP-001",
                        JsonNodeFactory.instance.objectNode().put("uid", "user-001")
                ).withMemberId("mem-001")
        );

        assertThat(response.path("loanId").asText()).isEqualTo("LN-001");

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> captor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        assertThat(captor.getValue().requestId()).isEqualTo("LQ-001");
        assertThat(captor.getValue().path()).isEqualTo("/loan/query");
        assertThat(captor.getValue().bizOrderNo()).isEqualTo("APP-001");
    }

    @Test
    void shouldConvertMissingDataIntoEmptyObjectNode() {
        YunkaCallTemplate template = new YunkaCallTemplate(yunkaGatewayClient);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", null));

        JsonNode response = template.executeForData(
                YunkaCallTemplate.YunkaCall.of("repay trial", "RT-001", "/repay/trial", "LN-001", new Object())
        );

        assertThat(response.isObject()).isTrue();
        assertThat(response.isEmpty()).isTrue();
    }

    @Test
    void shouldThrowBizExceptionWhenStrictDataCallIsRejected() {
        YunkaCallTemplate template = new YunkaCallTemplate(yunkaGatewayClient);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(10003, "invalid state", null));

        assertThatThrownBy(() -> template.executeForData(
                YunkaCallTemplate.YunkaCall.of("benefit sync", "SYNC-001", "/benefit/sync", "BEN-001", new Object())
        )).isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo(ErrorCodes.YUNKA_UPSTREAM_REJECTED);
    }

    @Test
    void shouldExposeRawResponseForCustomPostProcessing() {
        YunkaCallTemplate template = new YunkaCallTemplate(yunkaGatewayClient);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(7002, "processing", JsonNodeFactory.instance.objectNode()));

        YunkaGatewayClient.YunkaGatewayResponse response = template.execute(
                YunkaCallTemplate.YunkaCall.of("loan apply", "LA-001", "/loan/apply", "APP-001", new Object())
        );

        assertThat(response.code()).isEqualTo(7002);
        assertThat(response.message()).isEqualTo("processing");
        assertThat(template.isSuccessful(response)).isFalse();
        assertThat(template.hasData(response)).isFalse();
    }
}
