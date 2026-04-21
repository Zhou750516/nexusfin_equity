package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AllinpayDirectQwBenefitClient implements QwBenefitClient {

    private static final DateTimeFormatter REQUEST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;
    private final QwProperties properties;
    private final AllinpayCertificateLoader certificateLoader;
    private final AllinpayDirectRequestFactory requestFactory;
    private final AllinpayDirectPayloadMapperRegistry payloadMapperRegistry;
    private final AllinpayDirectProtocolSerializer protocolSerializer;
    private final AllinpayDirectTransportMapper transportMapper;
    private final AllinpayDirectHttpExecutor httpExecutor;
    private final AllinpayDirectResponseVerificationStage responseVerificationStage;
    private final AllinpayDirectResponseParser responseParser;
    private final AllinpayDirectEnvelopeFactory envelopeFactory;
    private final AllinpaySslContextFactory sslContextFactory = new AllinpaySslContextFactory();
    private final AllinpayRestClientFactory restClientFactory = new AllinpayRestClientFactory();
    private volatile KeyStore merchantKeyStore;
    private volatile X509Certificate verifyCertificate;
    private volatile AllinpayRequestSigner requestSigner;
    private volatile AllinpayDirectRequestPreparer requestPreparer;
    @SuppressWarnings("unused")
    private volatile AllinpayResponseVerifier responseVerifier;
    @SuppressWarnings("unused")
    private volatile RestClient restClient;

    @Autowired
    public AllinpayDirectQwBenefitClient(
            QwProperties properties,
            ObjectMapper objectMapper,
            AllinpayCertificateLoader certificateLoader,
            AllinpayDirectRequestFactory requestFactory,
            AllinpayDirectPayloadMapperRegistry payloadMapperRegistry,
            AllinpayDirectEnvelopeFactory envelopeFactory,
            AllinpayDirectProtocolSerializer protocolSerializer,
            AllinpayDirectTransportMapper transportMapper,
            AllinpayDirectHttpExecutor httpExecutor,
            AllinpayDirectResponseVerificationStage responseVerificationStage,
            AllinpayDirectResponseParser responseParser
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.certificateLoader = certificateLoader;
        this.requestFactory = requestFactory;
        this.payloadMapperRegistry = payloadMapperRegistry;
        this.envelopeFactory = envelopeFactory;
        this.protocolSerializer = protocolSerializer;
        this.transportMapper = transportMapper;
        this.httpExecutor = httpExecutor;
        this.responseVerificationStage = responseVerificationStage;
        this.responseParser = responseParser;
    }

    public AllinpayDirectQwBenefitClient(
            QwProperties properties,
            ObjectMapper objectMapper,
            AllinpayCertificateLoader certificateLoader
    ) {
        this(
                properties,
                objectMapper,
                certificateLoader,
                new AllinpayDirectRequestFactory(properties),
                defaultPayloadMapperRegistry(objectMapper),
                new AllinpayDirectEnvelopeFactory(),
                new AllinpayDirectSkeletonProtocolSerializer(objectMapper),
                new AllinpayDirectSkeletonTransportMapper(),
                new AllinpayDirectSkeletonHttpExecutor(),
                new AllinpayDirectSkeletonResponseVerificationStage(),
                new AllinpayDirectSkeletonResponseParser()
        );
    }

    private static AllinpayDirectPayloadMapperRegistry defaultPayloadMapperRegistry(ObjectMapper objectMapper) {
        return new AllinpayDirectPayloadMapperRegistry(
                objectMapper,
                new AllinpayMemberSyncPayloadMapper(),
                new AllinpayExerciseUrlPayloadMapper(),
                new AllinpayLendingNotifyPayloadMapper()
        );
    }

    @Override
    public QwMemberSyncResponse syncMemberOrder(QwMemberSyncRequest request) {
        ensureReady("memberSync", properties.getDirect().getMemberSyncServiceCode());
        AllinpayDirectInvocation invocation = requestFactory.prepareMemberSync(request);
        return execute(invocation, QwMemberSyncResponse.class);
    }

    @Override
    public QwExerciseUrlResponse getExerciseUrl(QwExerciseUrlRequest request) {
        ensureReady("exerciseUrl", properties.getDirect().getExerciseUrlServiceCode());
        AllinpayDirectInvocation invocation = requestFactory.prepareExerciseUrl(request);
        return execute(invocation, QwExerciseUrlResponse.class);
    }

    @Override
    public QwLendingNotifyResponse notifyLending(QwLendingNotifyRequest request) {
        ensureReady("lendingNotify", properties.getDirect().getLendingNotifyServiceCode());
        AllinpayDirectInvocation invocation = requestFactory.prepareLendingNotify(request);
        return execute(invocation, QwLendingNotifyResponse.class);
    }

    @Override
    public QwSignStatusResponse querySignStatus(QwSignStatusRequest request) {
        throw new BizException("ALLINPAY_DIRECT_UNSUPPORTED", "Allinpay direct mode does not support sign status query");
    }

    @Override
    public QwSignApplyResponse applySign(QwSignApplyRequest request) {
        throw new BizException("ALLINPAY_DIRECT_UNSUPPORTED", "Allinpay direct mode does not support sign apply");
    }

    @Override
    public QwSignConfirmResponse confirmSign(QwSignConfirmRequest request) {
        throw new BizException("ALLINPAY_DIRECT_UNSUPPORTED", "Allinpay direct mode does not support sign confirm");
    }

    private void ensureReady(String action, String serviceCode) {
        if (!properties.isEnabled()) {
            throw new BizException("QW_DISABLED", "QW integration is disabled");
        }
        requireNotBlank(properties.getDirect().getBaseUrl(), "direct.baseUrl");
        requireNotBlank(properties.getDirect().getProcessPath(), "direct.processPath");
        requireNotBlank(properties.getDirect().getPkcs12Path(), "direct.pkcs12Path");
        requireNotBlank(properties.getDirect().getPkcs12Password(), "direct.pkcs12Password");
        requireNotBlank(properties.getDirect().getVerifyCertPath(), "direct.verifyCertPath");
        requireNotBlank(serviceCode, "direct." + action + "ServiceCode");
        loadMaterialsIfNecessary();
    }

    private void loadMaterialsIfNecessary() {
        if (merchantKeyStore != null && verifyCertificate != null) {
            return;
        }
        synchronized (this) {
            if (merchantKeyStore == null) {
                merchantKeyStore = certificateLoader.loadPkcs12(
                        properties.getDirect().getPkcs12Path(),
                        properties.getDirect().getPkcs12Password()
                );
            }
            if (verifyCertificate == null) {
                verifyCertificate = certificateLoader.loadCertificate(properties.getDirect().getVerifyCertPath());
            }
            if (requestSigner == null) {
                requestSigner = new AllinpayRequestSigner(merchantKeyStore, properties.getDirect().getPkcs12Password());
            }
            if (requestPreparer == null) {
                requestPreparer = new AllinpayDirectRequestPreparer(protocolSerializer, requestSigner);
            }
            if (responseVerifier == null) {
                responseVerifier = new AllinpayResponseVerifier(verifyCertificate);
            }
            if (restClient == null) {
                restClient = restClientFactory.create(
                        sslContextFactory.create(
                                merchantKeyStore,
                                properties.getDirect().getPkcs12Password(),
                                verifyCertificate
                        ),
                        properties.getDirect().getConnectTimeoutMs(),
                        properties.getDirect().getReadTimeoutMs()
                );
            }
        }
    }

    private void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(
                    "ALLINPAY_DIRECT_CONFIG_MISSING",
                    "Missing required allinpay direct configuration: " + field
            );
        }
    }

    private AllinpayDirectEnvelope buildEnvelope(AllinpayDirectInvocation invocation) {
        return envelopeFactory.create(
                invocation,
                payloadMapperRegistry.map(invocation.operation(), invocation.businessRequest()),
                REQUEST_TIME_FORMATTER.format(LocalDateTime.now())
        );
    }

    private <T> T execute(AllinpayDirectInvocation invocation, Class<T> responseType) {
        AllinpayDirectEnvelope envelope = buildEnvelope(invocation);
        AllinpayDirectPreparedRequest preparedRequest = requestPreparer.prepare(envelope);
        AllinpayDirectTransportRequest transportRequest = transportMapper.map(preparedRequest);
        AllinpayDirectRawResponse rawResponse;
        try {
            rawResponse = httpExecutor.execute(transportRequest);
        } catch (BizException exception) {
            throw enrichProtocolBoundary(invocation, preparedRequest, exception);
        }
        AllinpayDirectVerifiedResponse verifiedResponse;
        try {
            verifiedResponse = responseVerificationStage.verify(rawResponse);
        } catch (BizException exception) {
            throw enrichProtocolBoundary(invocation, preparedRequest, exception);
        }
        return responseParser.parse(
                invocation.operation(),
                invocation.serviceCode(),
                verifiedResponse,
                responseType
        );
    }

    private BizException enrichProtocolBoundary(
            AllinpayDirectInvocation invocation,
            AllinpayDirectPreparedRequest preparedRequest,
            BizException exception
    ) {
        if (!exception.getMessage().contains("ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED")) {
            return exception;
        }
        return new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct signed request is prepared for "
                        + invocation.operation()
                        + " with serviceCode="
                        + invocation.serviceCode()
                        + " to "
                        + preparedRequest.targetUri()
                        + " but real protocol request/response mapping is not implemented"
        );
    }
}
