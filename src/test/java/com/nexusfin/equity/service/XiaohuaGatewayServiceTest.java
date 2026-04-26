package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.service.impl.XiaohuaGatewayServiceImpl;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncRequest;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import org.junit.jupiter.api.BeforeEach;
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
class XiaohuaGatewayServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private YunkaGatewayClient yunkaGatewayClient;

    private XiaohuaGatewayService gatewayService;

    @BeforeEach
    void setUp() {
        gatewayService = new XiaohuaGatewayServiceImpl(yunkaGatewayClient, yunkaProperties(), objectMapper);
    }

    @Test
    void shouldQueryProtocolsUsingConfiguredPath() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {
                  "list": [
                    {"title": "借款协议", "isShow": 1, "url": "https://agreements/loan"},
                    {"title": "服务协议", "isShow": 2, "url": "https://agreements/service"}
                  ]
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", data));

        ProtocolQueryResponse response = gatewayService.queryProtocols(
                "REQ-PROTO-001",
                "BIZ-PROTO-001",
                new ProtocolQueryRequest("user-001", 300000L, 3)
        );

        assertThat(response.list()).hasSize(2);
        assertThat(response.list().get(0).title()).isEqualTo("借款协议");
        assertThat(response.list().get(1).url()).isEqualTo("https://agreements/service");

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> captor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        assertThat(captor.getValue().path()).isEqualTo("/protocol/queryProtocolAggregationLink");
        assertThat(captor.getValue().bizOrderNo()).isEqualTo("BIZ-PROTO-001");
        JsonNode forwarded = objectMapper.valueToTree(captor.getValue().data());
        assertThat(forwarded.path("userId").asText()).isEqualTo("user-001");
        assertThat(forwarded.path("loanAmount").asLong()).isEqualTo(300000L);
    }

    @Test
    void shouldQueryUserCardsUsingConfiguredPath() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {
                  "list": [
                    {"cardId": "card-001", "bankName": "招商银行", "cardLastFour": "8648", "isDefault": 1}
                  ]
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", data));

        UserCardListResponse response = gatewayService.queryUserCards(
                "REQ-CARD-001",
                "BIZ-CARD-001",
                new UserCardListRequest("user-001")
        );

        assertThat(response.cards()).hasSize(1);
        assertThat(response.cards().get(0).bankName()).isEqualTo("招商银行");
        assertThat(response.cards().get(0).cardLastFour()).isEqualTo("8648");

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> captor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        assertThat(captor.getValue().path()).isEqualTo("/card/userCards");
    }

    @Test
    void shouldThrowBizExceptionWhenBenefitSyncIsRejected() {
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(10003, "invalid benefit status", null));

        assertThatThrownBy(() -> gatewayService.syncBenefitOrder(
                "REQ-SYNC-001",
                "BIZ-SYNC-001",
                new BenefitOrderSyncRequest("user-001", "BEN-001", "ACTIVE", 30000L)
        )).isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo(ErrorCodes.YUNKA_UPSTREAM_REJECTED);
    }

    private YunkaProperties yunkaProperties() {
        return new YunkaProperties(
                true,
                "REST",
                "https://yunka.test",
                "/api/gateway/proxy",
                2000,
                3000,
                new YunkaProperties.Paths(
                        "/loan/trail",
                        "/loan/query",
                        "/loan/apply",
                        "/repay/trial",
                        "/repay/apply",
                        "/repay/query",
                        "/protocol/queryProtocolAggregationLink",
                        "/user/token",
                        "/user/query",
                        "/loan/repayPlan",
                        "/card/smsSend",
                        "/card/smsConfirm",
                        "/card/userCards",
                        "/credit/image/query",
                        "/benefit/sync"
                )
        );
    }
}
