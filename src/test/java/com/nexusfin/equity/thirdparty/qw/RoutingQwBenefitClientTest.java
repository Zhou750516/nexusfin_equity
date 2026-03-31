package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.config.QwProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingQwBenefitClientTest {

    @Mock
    private QwBenefitClientImpl qweimobileClient;

    @Mock
    private AllinpayDirectQwBenefitClient allinpayDirectClient;

    @Test
    void shouldRouteMockModeToQweimobileClient() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        RoutingQwBenefitClient routingClient = new RoutingQwBenefitClient(properties, qweimobileClient, allinpayDirectClient);
        QwMemberSyncResponse expected = new QwMemberSyncResponse("qw-order-1", "card-1", "1", 0,
                "P-1", "权益产品", "independence", "2026-03-31 00:00:00", "2027-03-31 00:00:00");
        when(qweimobileClient.syncMemberOrder(any())).thenReturn(expected);

        QwMemberSyncResponse response = routingClient.syncMemberOrder(new QwMemberSyncRequest(
                "user-1", "ord-1", 680000L, "P-1", "权益产品", "13800138000", "张三", "proto-1",
                null, 0, null, null, null, null
        ));

        assertThat(response).isSameAs(expected);
        verify(qweimobileClient).syncMemberOrder(any());
    }

    @Test
    void shouldRouteQweimobileHttpModeToQweimobileClient() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.QWEIMOBILE_HTTP);
        RoutingQwBenefitClient routingClient = new RoutingQwBenefitClient(properties, qweimobileClient, allinpayDirectClient);
        QwExerciseUrlResponse expected = new QwExerciseUrlResponse(0, "https://redirect.test", "token-1",
                "2026-03-31 00:00:00", "2027-03-31 00:00:00");
        when(qweimobileClient.getExerciseUrl(any())).thenReturn(expected);

        QwExerciseUrlResponse response = routingClient.getExerciseUrl(new QwExerciseUrlRequest("user-1", "ord-1"));

        assertThat(response).isSameAs(expected);
        verify(qweimobileClient).getExerciseUrl(any());
    }

    @Test
    void shouldRouteAllinpayDirectModeToDirectClient() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.ALLINPAY_DIRECT);
        RoutingQwBenefitClient routingClient = new RoutingQwBenefitClient(properties, qweimobileClient, allinpayDirectClient);
        QwLendingNotifyResponse expected = new QwLendingNotifyResponse("qw-order-1");
        when(allinpayDirectClient.notifyLending(any())).thenReturn(expected);

        QwLendingNotifyResponse response = routingClient.notifyLending(new QwLendingNotifyRequest("user-1", "ord-1", 1));

        assertThat(response).isSameAs(expected);
        verify(allinpayDirectClient).notifyLending(any());
    }
}
