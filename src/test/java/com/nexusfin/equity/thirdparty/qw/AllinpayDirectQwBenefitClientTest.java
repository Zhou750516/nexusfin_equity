package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.HttpMethod;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AllinpayDirectQwBenefitClientTest {

    @Mock
    private AllinpayCertificateLoader certificateLoader;

    @Test
    void shouldRejectMissingDirectCertificateConfig() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.ALLINPAY_DIRECT);
        properties.getDirect().setMemberSyncServiceCode("SYNC001");

        AllinpayDirectQwBenefitClient client = new AllinpayDirectQwBenefitClient(
                properties,
                new ObjectMapper(),
                certificateLoader
        );

        assertThatThrownBy(() -> client.syncMemberOrder(new QwMemberSyncRequest(
                "user-1", "ord-1", 680000L, "P-1", "权益产品", "13800138000", "张三", "proto-1",
                null, 0, null, null, null, null
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ALLINPAY_DIRECT_CONFIG_MISSING");

        verify(certificateLoader, never()).loadPkcs12(anyString(), anyString());
    }

    @Test
    void shouldLoadCertificatesBeforeReportingProtocolUnimplemented() throws Exception {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.ALLINPAY_DIRECT);
        properties.setEnabled(true);
        properties.getDirect().setBaseUrl("https://tlt-test.allinpay.com");
        properties.getDirect().setProcessPath("/aipg/ProcessServlet");
        properties.getDirect().setMerchantId("200000000007804");
        properties.getDirect().setUserName("20000000000780404");
        properties.getDirect().setUserPassword("111111");
        properties.getDirect().setPkcs12Path("docs/third-part/齐为/通联测试证书/user-rsa.p12");
        properties.getDirect().setPkcs12Password("111111");
        properties.getDirect().setVerifyCertPath("docs/third-part/齐为/通联测试证书/public-rsa.cer");
        properties.getDirect().setMemberSyncServiceCode("SYNC001");

        AllinpayDirectQwBenefitClient client = new AllinpayDirectQwBenefitClient(
                properties,
                new ObjectMapper(),
                new AllinpayCertificateLoader()
        );

        assertThatThrownBy(() -> client.syncMemberOrder(new QwMemberSyncRequest(
                "user-1", "ord-1", 680000L, "P-1", "权益产品", "13800138000", "张三", "proto-1",
                null, 0, null, null, null, null
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED");
    }

    @Test
    void shouldDelegatePreparedRequestToExecutorAndParser() {
        QwProperties properties = buildDirectProperties();
        AtomicReference<AllinpayDirectTransportRequest> executedRequest = new AtomicReference<>();
        AllinpayDirectProtocolSerializer serializer = envelope -> new AllinpayDirectSerializedRequest(
                envelope.targetUri(),
                MediaType.APPLICATION_JSON,
                "{\"phase\":\"serialized\"}",
                "{\"phase\":\"serialized\"}"
        );
        AllinpayDirectTransportMapper transportMapper = preparedRequest -> new AllinpayDirectTransportRequest(
                preparedRequest.targetUri(),
                HttpMethod.POST,
                preparedRequest.contentType(),
                preparedRequest.requestBody(),
                java.util.Map.of("X-Signature", preparedRequest.signature()),
                java.util.Map.of("signature", preparedRequest.signature())
        );
        AllinpayDirectHttpExecutor httpExecutor = transportRequest -> {
            executedRequest.set(transportRequest);
            return new AllinpayDirectRawResponse(200, "{\"phase\":\"raw\"}", "resp-signature");
        };
        AllinpayDirectResponseVerificationStage verificationStage = rawResponse -> {
            assertThat(rawResponse.responseBody()).isEqualTo("{\"phase\":\"raw\"}");
            return new AllinpayDirectVerifiedResponse(
                    rawResponse.httpStatus(),
                    rawResponse.responseBody(),
                    "verified-signature"
            );
        };
        AllinpayDirectResponseParser responseParser = new AllinpayDirectResponseParser() {
            @Override
            public <T> T parse(
                    AllinpayDirectOperation operation,
                    String serviceCode,
                    AllinpayDirectVerifiedResponse verifiedResponse,
                    Class<T> responseType
            ) {
                assertThat(operation).isEqualTo(AllinpayDirectOperation.MEMBER_SYNC);
                assertThat(serviceCode).isEqualTo("SYNC001");
                assertThat(verifiedResponse.responseBody()).isEqualTo("{\"phase\":\"raw\"}");
                assertThat(verifiedResponse.signature()).isEqualTo("verified-signature");
                return responseType.cast(new QwMemberSyncResponse(
                        "qw-ord-1", "card-1", "1711886400", 0, "P-1", "权益产品", "independence",
                        "2026-03-31 20:30:00", "2027-03-31 20:30:00"
                ));
            }
        };

        AllinpayDirectQwBenefitClient client = new AllinpayDirectQwBenefitClient(
                properties,
                new ObjectMapper(),
                new AllinpayCertificateLoader(),
                new AllinpayDirectRequestFactory(properties),
                new AllinpayDirectPayloadMapperRegistry(
                        new ObjectMapper(),
                        new AllinpayMemberSyncPayloadMapper(),
                        new AllinpayExerciseUrlPayloadMapper(),
                        new AllinpayLendingNotifyPayloadMapper()
                ),
                new AllinpayDirectEnvelopeFactory(),
                serializer,
                transportMapper,
                httpExecutor,
                verificationStage,
                responseParser
        );

        QwMemberSyncResponse response = client.syncMemberOrder(new QwMemberSyncRequest(
                "user-1", "ord-1", 680000L, "P-1", "权益产品", "13800138000", "张三", "proto-1",
                null, 0, null, null, null, null
        ));

        assertThat(response.orderNo()).isEqualTo("qw-ord-1");
        assertThat(executedRequest.get()).isNotNull();
        assertThat(executedRequest.get().method()).isEqualTo(HttpMethod.POST);
        assertThat(executedRequest.get().body()).isEqualTo("{\"phase\":\"serialized\"}");
        assertThat(executedRequest.get().headers()).containsKey("X-Signature");
        assertThat(executedRequest.get().attributes()).containsKey("signature");
    }

    private QwProperties buildDirectProperties() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.ALLINPAY_DIRECT);
        properties.setEnabled(true);
        properties.getDirect().setBaseUrl("https://tlt-test.allinpay.com");
        properties.getDirect().setProcessPath("/aipg/ProcessServlet");
        properties.getDirect().setMerchantId("200000000007804");
        properties.getDirect().setUserName("20000000000780404");
        properties.getDirect().setUserPassword("111111");
        properties.getDirect().setPkcs12Path("docs/third-part/齐为/通联测试证书/user-rsa.p12");
        properties.getDirect().setPkcs12Password("111111");
        properties.getDirect().setVerifyCertPath("docs/third-part/齐为/通联测试证书/public-rsa.cer");
        properties.getDirect().setMemberSyncServiceCode("SYNC001");
        properties.getDirect().setExerciseUrlServiceCode("TOKEN001");
        properties.getDirect().setLendingNotifyServiceCode("NOTIFY001");
        return properties;
    }
}
