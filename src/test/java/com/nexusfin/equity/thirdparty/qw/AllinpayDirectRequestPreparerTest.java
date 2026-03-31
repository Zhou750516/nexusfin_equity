package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import java.security.KeyStore;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayDirectRequestPreparerTest {

    private final AllinpayCertificateLoader certificateLoader = new AllinpayCertificateLoader();

    @Test
    void shouldPrepareSignedSkeletonRequestFromEnvelope() {
        QwProperties properties = new QwProperties();
        properties.getDirect().setBaseUrl("https://tlt-test.allinpay.com");
        properties.getDirect().setProcessPath("/aipg/ProcessServlet");
        properties.getDirect().setMerchantId("200000000007804");
        properties.getDirect().setUserName("20000000000780404");
        properties.getDirect().setUserPassword("111111");
        properties.getDirect().setMemberSyncServiceCode("SYNC001");
        AllinpayDirectRequestFactory requestFactory = new AllinpayDirectRequestFactory(properties);
        AllinpayDirectInvocation invocation = requestFactory.prepareMemberSync(new QwMemberSyncRequest(
                "user-1", "ord-1", 680000L, "P-1", "权益产品", "13800138000", "张三", "proto-1",
                null, 0, null, null, null, null
        ));
        AllinpayDirectPayloadMapperRegistry registry = new AllinpayDirectPayloadMapperRegistry(
                new ObjectMapper(),
                new AllinpayMemberSyncPayloadMapper(),
                new AllinpayExerciseUrlPayloadMapper(),
                new AllinpayLendingNotifyPayloadMapper()
        );
        AllinpayDirectEnvelope envelope = new AllinpayDirectEnvelopeFactory().create(
                invocation,
                registry.map(invocation.operation(), invocation.businessRequest()),
                "2026-03-31 22:00:00"
        );
        KeyStore keyStore = certificateLoader.loadPkcs12(
                "docs/third-part/齐为/通联测试证书/user-rsa.p12",
                "111111"
        );
        AllinpayRequestSigner signer = new AllinpayRequestSigner(keyStore, "111111");
        AllinpayDirectRequestPreparer preparer = new AllinpayDirectRequestPreparer(
                new AllinpayDirectSkeletonProtocolSerializer(new ObjectMapper()),
                signer
        );

        AllinpayDirectPreparedRequest preparedRequest = preparer.prepare(envelope);

        assertThat(preparedRequest.targetUri().toString()).isEqualTo("https://tlt-test.allinpay.com/aipg/ProcessServlet");
        assertThat(preparedRequest.contentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(preparedRequest.requestBody()).contains("\"merchantId\":\"200000000007804\"");
        assertThat(preparedRequest.signature()).isNotBlank();
    }
}
