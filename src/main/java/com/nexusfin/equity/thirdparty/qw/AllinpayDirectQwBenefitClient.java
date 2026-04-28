package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AllinpayDirectQwBenefitClient implements QwBenefitClient {

    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;
    private final QwProperties properties;
    private final AllinpayCertificateLoader certificateLoader;
    private final AllinpayDirectProtocolSerializer protocolSerializer;
    private final AllinpayDirectTransportMapper transportMapper;
    private final AllinpayDirectHttpExecutor httpExecutor;
    private final AllinpayDirectResponseVerificationStage responseVerificationStage;
    private final AllinpayDirectResponseParser responseParser;
    private final AllinpaySslContextFactory sslContextFactory = new AllinpaySslContextFactory();
    private final AllinpayRestClientFactory restClientFactory = new AllinpayRestClientFactory();
    private volatile KeyStore merchantKeyStore;
    private volatile X509Certificate verifyCertificate;
    private volatile AllinpayRequestSigner requestSigner;
    private volatile AllinpayDirectRequestBuilder requestBuilder;
    @SuppressWarnings("unused")
    private volatile AllinpayResponseVerifier responseVerifier;
    @SuppressWarnings("unused")
    private volatile RestClient restClient;

    @Autowired
    public AllinpayDirectQwBenefitClient(
            QwProperties properties,
            ObjectMapper objectMapper,
            AllinpayCertificateLoader certificateLoader,
            AllinpayDirectRequestBuilder requestBuilder,
            AllinpayDirectProtocolSerializer protocolSerializer,
            AllinpayDirectTransportMapper transportMapper,
            AllinpayDirectHttpExecutor httpExecutor,
            AllinpayDirectResponseVerificationStage responseVerificationStage,
            AllinpayDirectResponseParser responseParser
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.certificateLoader = certificateLoader;
        this.requestBuilder = requestBuilder;
        this.protocolSerializer = protocolSerializer;
        this.transportMapper = transportMapper;
        this.httpExecutor = httpExecutor;
        this.responseVerificationStage = responseVerificationStage;
        this.responseParser = responseParser;
    }

    @Autowired
    public AllinpayDirectQwBenefitClient(
            QwProperties properties,
            ObjectMapper objectMapper,
            AllinpayCertificateLoader certificateLoader,
            AllinpayDirectProtocolSerializer protocolSerializer,
            AllinpayDirectTransportMapper transportMapper,
            AllinpayDirectHttpExecutor httpExecutor,
            AllinpayDirectResponseVerificationStage responseVerificationStage,
            AllinpayDirectResponseParser responseParser
    ) {
        this(
                properties,
                objectMapper,
                certificateLoader,
                null,
                protocolSerializer,
                transportMapper,
                httpExecutor,
                responseVerificationStage,
                responseParser
        );
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
                new AllinpayDirectSkeletonProtocolSerializer(objectMapper),
                new AllinpayDirectSkeletonTransportMapper(),
                new AllinpayDirectUnsupportedProtocolHandler(),
                new AllinpayDirectUnsupportedProtocolHandler(),
                new AllinpayDirectUnsupportedProtocolHandler()
        );
    }

    @Override
    public QwMemberSyncResponse syncMemberOrder(QwMemberSyncRequest request) {
        ensureReady("memberSync", properties.getDirect().getMemberSyncServiceCode());
        AllinpayDirectPreparedRequest preparedRequest = requestBuilder.prepareMemberSync(request);
        return execute(
                AllinpayDirectOperation.MEMBER_SYNC,
                properties.getDirect().getMemberSyncServiceCode(),
                preparedRequest,
                QwMemberSyncResponse.class
        );
    }

    @Override
    public QwExerciseUrlResponse getExerciseUrl(QwExerciseUrlRequest request) {
        ensureReady("exerciseUrl", properties.getDirect().getExerciseUrlServiceCode());
        AllinpayDirectPreparedRequest preparedRequest = requestBuilder.prepareExerciseUrl(request);
        return execute(
                AllinpayDirectOperation.EXERCISE_URL,
                properties.getDirect().getExerciseUrlServiceCode(),
                preparedRequest,
                QwExerciseUrlResponse.class
        );
    }

    @Override
    public QwLendingNotifyResponse notifyLending(QwLendingNotifyRequest request) {
        ensureReady("lendingNotify", properties.getDirect().getLendingNotifyServiceCode());
        AllinpayDirectPreparedRequest preparedRequest = requestBuilder.prepareLendingNotify(request);
        return execute(
                AllinpayDirectOperation.LENDING_NOTIFY,
                properties.getDirect().getLendingNotifyServiceCode(),
                preparedRequest,
                QwLendingNotifyResponse.class
        );
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
            if (requestBuilder == null) {
                requestBuilder = new AllinpayDirectRequestBuilder(
                        properties,
                        objectMapper,
                        requestSigner::sign,
                        protocolSerializer,
                        new AllinpayMemberSyncPayloadMapper(),
                        new AllinpayExerciseUrlPayloadMapper(),
                        new AllinpayLendingNotifyPayloadMapper()
                );
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

    private <T> T execute(
            AllinpayDirectOperation operation,
            String serviceCode,
            AllinpayDirectPreparedRequest preparedRequest,
            Class<T> responseType
    ) {
        AllinpayDirectTransportRequest transportRequest = transportMapper.map(preparedRequest);
        AllinpayDirectRawResponse rawResponse;
        try {
            rawResponse = httpExecutor.execute(transportRequest);
        } catch (BizException exception) {
            throw enrichProtocolBoundary(operation, serviceCode, preparedRequest, exception);
        }
        AllinpayDirectVerifiedResponse verifiedResponse;
        try {
            verifiedResponse = responseVerificationStage.verify(rawResponse);
        } catch (BizException exception) {
            throw enrichProtocolBoundary(operation, serviceCode, preparedRequest, exception);
        }
        return responseParser.parse(
                operation,
                serviceCode,
                verifiedResponse,
                responseType
        );
    }

    private BizException enrichProtocolBoundary(
            AllinpayDirectOperation operation,
            String serviceCode,
            AllinpayDirectPreparedRequest preparedRequest,
            BizException exception
    ) {
        if (!exception.getMessage().contains("ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED")) {
            return exception;
        }
        return new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct signed request is prepared for "
                        + operation
                        + " with serviceCode="
                        + serviceCode
                        + " to "
                        + preparedRequest.targetUri()
                        + " but real protocol request/response mapping is not implemented"
        );
    }
}
