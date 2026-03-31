package com.nexusfin.equity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectHttpExecutor;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectEnvelopeFactory;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectPayloadMapperRegistry;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectProtocolSerializer;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectRequestFactory;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectResponseParser;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectResponseVerificationStage;
import com.nexusfin.equity.thirdparty.qw.AllinpayExerciseUrlPayloadMapper;
import com.nexusfin.equity.thirdparty.qw.AllinpayLendingNotifyPayloadMapper;
import com.nexusfin.equity.thirdparty.qw.AllinpayMemberSyncPayloadMapper;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectSkeletonHttpExecutor;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectSkeletonProtocolSerializer;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectSkeletonResponseParser;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectSkeletonResponseVerificationStage;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectSkeletonTransportMapper;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectTransportMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AllinpayDirectConfiguration {

    @Bean
    @ConditionalOnMissingBean(AllinpayDirectRequestFactory.class)
    public AllinpayDirectRequestFactory allinpayDirectRequestFactory(QwProperties properties) {
        return new AllinpayDirectRequestFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean(AllinpayDirectPayloadMapperRegistry.class)
    public AllinpayDirectPayloadMapperRegistry allinpayDirectPayloadMapperRegistry(ObjectMapper objectMapper) {
        return new AllinpayDirectPayloadMapperRegistry(
                objectMapper,
                new AllinpayMemberSyncPayloadMapper(),
                new AllinpayExerciseUrlPayloadMapper(),
                new AllinpayLendingNotifyPayloadMapper()
        );
    }

    @Bean
    @ConditionalOnMissingBean(AllinpayDirectEnvelopeFactory.class)
    public AllinpayDirectEnvelopeFactory allinpayDirectEnvelopeFactory() {
        return new AllinpayDirectEnvelopeFactory();
    }

    @Bean
    @ConditionalOnMissingBean(AllinpayDirectProtocolSerializer.class)
    public AllinpayDirectProtocolSerializer allinpayDirectProtocolSerializer(ObjectMapper objectMapper) {
        return new AllinpayDirectSkeletonProtocolSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AllinpayDirectTransportMapper.class)
    public AllinpayDirectTransportMapper allinpayDirectTransportMapper() {
        return new AllinpayDirectSkeletonTransportMapper();
    }

    @Bean
    @ConditionalOnMissingBean(AllinpayDirectHttpExecutor.class)
    public AllinpayDirectHttpExecutor allinpayDirectHttpExecutor() {
        return new AllinpayDirectSkeletonHttpExecutor();
    }

    @Bean
    @ConditionalOnMissingBean(AllinpayDirectResponseVerificationStage.class)
    public AllinpayDirectResponseVerificationStage allinpayDirectResponseVerificationStage() {
        return new AllinpayDirectSkeletonResponseVerificationStage();
    }

    @Bean
    @ConditionalOnMissingBean(AllinpayDirectResponseParser.class)
    public AllinpayDirectResponseParser allinpayDirectResponseParser() {
        return new AllinpayDirectSkeletonResponseParser();
    }
}
