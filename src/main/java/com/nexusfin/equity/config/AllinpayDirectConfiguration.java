package com.nexusfin.equity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectHttpExecutor;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectProtocolSerializer;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectRequestBuilder;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectResponseParser;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectResponseVerificationStage;
import com.nexusfin.equity.thirdparty.qw.AllinpayExerciseUrlPayloadMapper;
import com.nexusfin.equity.thirdparty.qw.AllinpayLendingNotifyPayloadMapper;
import com.nexusfin.equity.thirdparty.qw.AllinpayMemberSyncPayloadMapper;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectSkeletonProtocolSerializer;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectSkeletonTransportMapper;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectTransportMapper;
import com.nexusfin.equity.thirdparty.qw.AllinpayDirectUnsupportedProtocolHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AllinpayDirectConfiguration {

    @Bean
    @ConditionalOnMissingBean(AllinpayDirectRequestBuilder.class)
    public AllinpayDirectRequestBuilder allinpayDirectRequestBuilder(
            QwProperties properties,
            ObjectMapper objectMapper,
            AllinpayDirectProtocolSerializer protocolSerializer
    ) {
        return new AllinpayDirectRequestBuilder(
                properties,
                objectMapper,
                payload -> {
                    throw new BizException(
                            "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                            "Allinpay direct request signing is not initialized"
                    );
                },
                protocolSerializer,
                new AllinpayMemberSyncPayloadMapper(),
                new AllinpayExerciseUrlPayloadMapper(),
                new AllinpayLendingNotifyPayloadMapper()
        );
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
    @ConditionalOnMissingBean({
            AllinpayDirectHttpExecutor.class,
            AllinpayDirectResponseVerificationStage.class,
            AllinpayDirectResponseParser.class
    })
    public AllinpayDirectUnsupportedProtocolHandler allinpayDirectUnsupportedProtocolHandler() {
        return new AllinpayDirectUnsupportedProtocolHandler();
    }
}
