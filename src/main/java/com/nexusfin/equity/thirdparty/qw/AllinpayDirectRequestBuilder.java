package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class AllinpayDirectRequestBuilder {

    private static final DateTimeFormatter REQUEST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final QwProperties properties;
    private final ObjectMapper objectMapper;
    private final Function<String, String> signer;
    private final AllinpayDirectProtocolSerializer protocolSerializer;
    private final List<AllinpayDirectPayloadMapper<?>> mappers;

    public AllinpayDirectRequestBuilder(
            QwProperties properties,
            ObjectMapper objectMapper,
            AllinpayRequestSigner requestSigner,
            AllinpayDirectProtocolSerializer protocolSerializer,
            AllinpayDirectPayloadMapper<?>... mappers
    ) {
        this(
                properties,
                objectMapper,
                requestSigner::sign,
                protocolSerializer,
                mappers
        );
    }

    public AllinpayDirectRequestBuilder(
            QwProperties properties,
            ObjectMapper objectMapper,
            AllinpayRequestSigner requestSigner,
            AllinpayDirectPayloadMapper<?>... mappers
    ) {
        this(
                properties,
                objectMapper,
                requestSigner::sign,
                new AllinpayDirectSkeletonProtocolSerializer(objectMapper),
                mappers
        );
    }

    public AllinpayDirectRequestBuilder(
            QwProperties properties,
            ObjectMapper objectMapper,
            Function<String, String> signer,
            AllinpayDirectProtocolSerializer protocolSerializer,
            AllinpayDirectPayloadMapper<?>... mappers
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.signer = signer;
        this.protocolSerializer = protocolSerializer;
        this.mappers = Arrays.asList(mappers);
    }

    public AllinpayDirectPreparedRequest prepareMemberSync(QwMemberSyncRequest request) {
        return prepare(buildInvocation(
                AllinpayDirectOperation.MEMBER_SYNC,
                properties.getDirect().getMemberSyncServiceCode(),
                request
        ));
    }

    public AllinpayDirectPreparedRequest prepareExerciseUrl(QwExerciseUrlRequest request) {
        return prepare(buildInvocation(
                AllinpayDirectOperation.EXERCISE_URL,
                properties.getDirect().getExerciseUrlServiceCode(),
                request
        ));
    }

    public AllinpayDirectPreparedRequest prepareLendingNotify(QwLendingNotifyRequest request) {
        return prepare(buildInvocation(
                AllinpayDirectOperation.LENDING_NOTIFY,
                properties.getDirect().getLendingNotifyServiceCode(),
                request
        ));
    }

    private AllinpayDirectPreparedRequest prepare(AllinpayDirectInvocation invocation) {
        AllinpayDirectEnvelope envelope = new AllinpayDirectEnvelope(
                invocation.operation(),
                invocation.targetUri(),
                new AllinpayDirectEnvelopeHead(
                        invocation.serviceCode(),
                        invocation.merchantId(),
                        invocation.userName(),
                        invocation.userPassword(),
                        REQUEST_TIME_FORMATTER.format(LocalDateTime.now())
                ),
                mapPayload(invocation.operation(), invocation.businessRequest())
        );
        AllinpayDirectSerializedRequest serializedRequest = protocolSerializer.serialize(envelope);
        return new AllinpayDirectPreparedRequest(
                serializedRequest.targetUri(),
                serializedRequest.contentType(),
                serializedRequest.requestBody(),
                signer.apply(serializedRequest.signingPayload())
        );
    }

    private AllinpayDirectInvocation buildInvocation(
            AllinpayDirectOperation operation,
            String serviceCode,
            Object businessRequest
    ) {
        requireNotBlank(properties.getDirect().getBaseUrl(), "direct.baseUrl");
        requireNotBlank(properties.getDirect().getProcessPath(), "direct.processPath");
        requireNotBlank(properties.getDirect().getMerchantId(), "direct.merchantId");
        requireNotBlank(properties.getDirect().getUserName(), "direct.userName");
        requireNotBlank(properties.getDirect().getUserPassword(), "direct.userPassword");
        requireNotBlank(serviceCode, serviceCodeField(operation));
        return new AllinpayDirectInvocation(
                operation,
                serviceCode,
                java.net.URI.create(properties.getDirect().getBaseUrl() + properties.getDirect().getProcessPath()),
                properties.getDirect().getMerchantId(),
                properties.getDirect().getUserName(),
                properties.getDirect().getUserPassword(),
                businessRequest
        );
    }

    private JsonNode mapPayload(AllinpayDirectOperation operation, Object request) {
        AllinpayDirectPayloadMapper<?> mapper = mappers.stream()
                .filter(candidate -> candidate.operation() == operation)
                .filter(candidate -> candidate.requestType().isInstance(request))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        "ALLINPAY_DIRECT_PAYLOAD_MAPPER_MISSING",
                        "Missing payload mapper for operation: " + operation
                ));
        return mapUnchecked(mapper, request);
    }

    @SuppressWarnings("unchecked")
    private <T> JsonNode mapUnchecked(AllinpayDirectPayloadMapper<T> mapper, Object request) {
        return mapper.map((T) request, objectMapper);
    }

    private String serviceCodeField(AllinpayDirectOperation operation) {
        return switch (operation) {
            case MEMBER_SYNC -> "direct.memberSyncServiceCode";
            case EXERCISE_URL -> "direct.exerciseUrlServiceCode";
            case LENDING_NOTIFY -> "direct.lendingNotifyServiceCode";
        };
    }

    private void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(
                    "ALLINPAY_DIRECT_CONFIG_MISSING",
                    "Missing required allinpay direct configuration: " + field
            );
        }
    }
}
