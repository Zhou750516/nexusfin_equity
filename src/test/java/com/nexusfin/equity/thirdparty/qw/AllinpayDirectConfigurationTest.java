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
    void shouldRegisterSkeletonProtocolBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AllinpayDirectRequestFactory.class);
            assertThat(context).hasSingleBean(AllinpayDirectPayloadMapperRegistry.class);
            assertThat(context).hasSingleBean(AllinpayDirectEnvelopeFactory.class);
            assertThat(context).hasSingleBean(AllinpayDirectProtocolSerializer.class);
            assertThat(context).hasSingleBean(AllinpayDirectTransportMapper.class);
            assertThat(context).hasSingleBean(AllinpayDirectHttpExecutor.class);
            assertThat(context).hasSingleBean(AllinpayDirectResponseVerificationStage.class);
            assertThat(context).hasSingleBean(AllinpayDirectResponseParser.class);

            assertThat(context.getBean(AllinpayDirectRequestFactory.class)).isNotNull();
            assertThat(context.getBean(AllinpayDirectPayloadMapperRegistry.class)).isNotNull();
            assertThat(context.getBean(AllinpayDirectEnvelopeFactory.class)).isNotNull();
            assertThat(context.getBean(AllinpayDirectProtocolSerializer.class))
                    .isInstanceOf(AllinpayDirectSkeletonProtocolSerializer.class);
            assertThat(context.getBean(AllinpayDirectTransportMapper.class))
                    .isInstanceOf(AllinpayDirectSkeletonTransportMapper.class);
            assertThat(context.getBean(AllinpayDirectHttpExecutor.class))
                    .isInstanceOf(AllinpayDirectSkeletonHttpExecutor.class);
            assertThat(context.getBean(AllinpayDirectResponseVerificationStage.class))
                    .isInstanceOf(AllinpayDirectSkeletonResponseVerificationStage.class);
            assertThat(context.getBean(AllinpayDirectResponseParser.class))
                    .isInstanceOf(AllinpayDirectSkeletonResponseParser.class);
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
                    assertThat(context.getBean(AllinpayDirectRequestFactory.class))
                            .isSameAs(CustomFactoryConfiguration.REQUEST_FACTORY);
                    assertThat(context.getBean(AllinpayDirectPayloadMapperRegistry.class))
                            .isSameAs(CustomFactoryConfiguration.PAYLOAD_MAPPER_REGISTRY);
                    assertThat(context.getBean(AllinpayDirectEnvelopeFactory.class))
                            .isSameAs(CustomFactoryConfiguration.ENVELOPE_FACTORY);
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

        private static final AllinpayDirectRequestFactory REQUEST_FACTORY =
                new AllinpayDirectRequestFactory(new com.nexusfin.equity.config.QwProperties());
        private static final AllinpayDirectPayloadMapperRegistry PAYLOAD_MAPPER_REGISTRY =
                new AllinpayDirectPayloadMapperRegistry(
                        new ObjectMapper(),
                        new AllinpayMemberSyncPayloadMapper(),
                        new AllinpayExerciseUrlPayloadMapper(),
                        new AllinpayLendingNotifyPayloadMapper()
                );
        private static final AllinpayDirectEnvelopeFactory ENVELOPE_FACTORY = new AllinpayDirectEnvelopeFactory();

        @Bean
        AllinpayDirectRequestFactory allinpayDirectRequestFactory() {
            return REQUEST_FACTORY;
        }

        @Bean
        AllinpayDirectPayloadMapperRegistry allinpayDirectPayloadMapperRegistry() {
            return PAYLOAD_MAPPER_REGISTRY;
        }

        @Bean
        AllinpayDirectEnvelopeFactory allinpayDirectEnvelopeFactory() {
            return ENVELOPE_FACTORY;
        }
    }
}
