package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.AllinpayDirectConfiguration;
import com.nexusfin.equity.config.QwProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayDirectConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BaseConfiguration.class, AllinpayDirectConfiguration.class);

    @Test
    void shouldRegisterConvergedDirectBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AllinpayDirectRequestBuilder.class);
            assertThat(context).hasSingleBean(AllinpayDirectProtocolSerializer.class);
            assertThat(context).hasSingleBean(AllinpayDirectTransportMapper.class);
            assertThat(context).hasSingleBean(AllinpayDirectHttpExecutor.class);
            assertThat(context).hasSingleBean(AllinpayDirectResponseVerificationStage.class);
            assertThat(context).hasSingleBean(AllinpayDirectResponseParser.class);

            assertThat(context.getBean(AllinpayDirectRequestBuilder.class)).isNotNull();
            assertThat(context.getBean(AllinpayDirectProtocolSerializer.class))
                    .isInstanceOf(AllinpayDirectJsonProtocolSerializer.class);
            assertThat(context.getBean(AllinpayDirectTransportMapper.class))
                    .isInstanceOf(AllinpayDirectSignatureTransportMapper.class);
            assertThat(context.getBean(AllinpayDirectHttpExecutor.class))
                    .isInstanceOf(AllinpayDirectUnsupportedProtocolHandler.class);
            assertThat(context.getBean(AllinpayDirectResponseVerificationStage.class))
                    .isInstanceOf(AllinpayDirectUnsupportedProtocolHandler.class);
            assertThat(context.getBean(AllinpayDirectResponseParser.class))
                    .isInstanceOf(AllinpayDirectUnsupportedProtocolHandler.class);
        });
    }

    @Test
    void shouldAllowProtocolBeansToBeOverridden() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        BaseConfiguration.class,
                        CustomProtocolConfiguration.class,
                        AllinpayDirectConfiguration.class
                )
                .run(context -> {
            assertThat(context.getBean(AllinpayDirectProtocolSerializer.class))
                    .isSameAs(CustomProtocolConfiguration.PROTOCOL_SERIALIZER);
            assertThat(context.getBean(AllinpayDirectTransportMapper.class))
                    .isSameAs(CustomProtocolConfiguration.TRANSPORT_MAPPER);
            assertThat(context.getBean(AllinpayDirectHttpExecutor.class))
                    .isSameAs(CustomProtocolConfiguration.HTTP_EXECUTOR);
            assertThat(context.getBean(AllinpayDirectResponseVerificationStage.class))
                    .isSameAs(CustomProtocolConfiguration.VERIFICATION_STAGE);
            assertThat(context.getBean(AllinpayDirectResponseParser.class))
                    .isSameAs(CustomProtocolConfiguration.RESPONSE_PARSER);
                });
    }

    @Test
    void shouldAllowFactoryBeansToBeOverridden() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        BaseConfiguration.class,
                        CustomFactoryConfiguration.class,
                        AllinpayDirectConfiguration.class
                )
                .run(context -> {
                    assertThat(context.getBean(AllinpayDirectRequestBuilder.class))
                            .isSameAs(CustomFactoryConfiguration.REQUEST_BUILDER);
                });
    }

    @Configuration
    static class BaseConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        QwProperties qwProperties() {
            return new QwProperties();
        }
    }

    @Configuration
    static class CustomProtocolConfiguration {

        private static final AllinpayDirectProtocolSerializer PROTOCOL_SERIALIZER = envelope ->
                new AllinpayDirectSerializedRequest(
                        envelope.targetUri(),
                        org.springframework.http.MediaType.APPLICATION_JSON,
                        "{\"custom\":true}",
                        "{\"custom\":true}"
                );
        private static final AllinpayDirectTransportMapper TRANSPORT_MAPPER = preparedRequest ->
                new AllinpayDirectTransportRequest(
                        preparedRequest.targetUri(),
                        org.springframework.http.HttpMethod.POST,
                        preparedRequest.contentType(),
                        preparedRequest.requestBody(),
                        java.util.Map.of("X-Test", "1"),
                        java.util.Map.of()
                );
        private static final AllinpayDirectHttpExecutor HTTP_EXECUTOR = transportRequest ->
                new AllinpayDirectRawResponse(200, "{\"ok\":true}", "sig");
        private static final AllinpayDirectResponseVerificationStage VERIFICATION_STAGE = rawResponse ->
                new AllinpayDirectVerifiedResponse(rawResponse.httpStatus(), rawResponse.responseBody(), "verified");
        private static final AllinpayDirectResponseParser RESPONSE_PARSER = new AllinpayDirectResponseParser() {
            @Override
            public <T> T parse(
                    AllinpayDirectOperation operation,
                    String serviceCode,
                    AllinpayDirectVerifiedResponse verifiedResponse,
                    Class<T> responseType
            ) {
                return null;
            }
        };

        @Bean
        AllinpayDirectProtocolSerializer allinpayDirectProtocolSerializer() {
            return PROTOCOL_SERIALIZER;
        }

        @Bean
        AllinpayDirectTransportMapper allinpayDirectTransportMapper() {
            return TRANSPORT_MAPPER;
        }

        @Bean
        AllinpayDirectHttpExecutor allinpayDirectHttpExecutor() {
            return HTTP_EXECUTOR;
        }

        @Bean
        AllinpayDirectResponseVerificationStage allinpayDirectResponseVerificationStage() {
            return VERIFICATION_STAGE;
        }

        @Bean
        AllinpayDirectResponseParser allinpayDirectResponseParser() {
            return RESPONSE_PARSER;
        }
    }

    @Configuration
    static class CustomFactoryConfiguration {

        private static final AllinpayDirectRequestBuilder REQUEST_BUILDER =
                new AllinpayDirectRequestBuilder(
                        new com.nexusfin.equity.config.QwProperties(),
                        new ObjectMapper(),
                        org.mockito.Mockito.mock(AllinpayRequestSigner.class),
                        new AllinpayMemberSyncPayloadMapper(),
                        new AllinpayExerciseUrlPayloadMapper(),
                        new AllinpayLendingNotifyPayloadMapper()
                );

        @Bean
        AllinpayDirectRequestBuilder allinpayDirectRequestBuilder() {
            return REQUEST_BUILDER;
        }
    }
}
