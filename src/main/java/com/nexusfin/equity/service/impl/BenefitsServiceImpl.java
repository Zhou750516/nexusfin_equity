package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.dto.request.BenefitsActivateRequest;
import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.response.BenefitsActivateResponse;
import com.nexusfin.equity.dto.response.BenefitsCardDetailResponse;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.service.BenefitOrderService;
import com.nexusfin.equity.service.BenefitsService;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncRequest;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardListRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

@Service
public class BenefitsServiceImpl implements BenefitsService {

    private final H5BenefitsProperties h5BenefitsProperties;
    private final H5LoanProperties h5LoanProperties;
    private final BenefitProductRepository benefitProductRepository;
    private final MemberPaymentProtocolRepository memberPaymentProtocolRepository;
    private final BenefitOrderService benefitOrderService;
    private final H5I18nService h5I18nService;
    private final XiaohuaGatewayService xiaohuaGatewayService;

    public BenefitsServiceImpl(
            H5BenefitsProperties h5BenefitsProperties,
            H5LoanProperties h5LoanProperties,
            BenefitProductRepository benefitProductRepository,
            MemberPaymentProtocolRepository memberPaymentProtocolRepository,
            BenefitOrderService benefitOrderService,
            H5I18nService h5I18nService,
            XiaohuaGatewayService xiaohuaGatewayService
    ) {
        this.h5BenefitsProperties = h5BenefitsProperties;
        this.h5LoanProperties = h5LoanProperties;
        this.benefitProductRepository = benefitProductRepository;
        this.memberPaymentProtocolRepository = memberPaymentProtocolRepository;
        this.benefitOrderService = benefitOrderService;
        this.h5I18nService = h5I18nService;
        this.xiaohuaGatewayService = xiaohuaGatewayService;
    }

    @Override
    public BenefitsCardDetailResponse getCardDetail(String memberId, String uid) {
        BenefitProduct product = benefitProductRepository.selectById(h5BenefitsProperties.productCode());
        if (product == null || !"ACTIVE".equals(product.getStatus())) {
            throw new BizException("PRODUCT_NOT_FOUND", "Benefit product not found");
        }
        H5BenefitsProperties.Detail detail = h5BenefitsProperties.detail();
        var protocolResponse = xiaohuaGatewayService.queryProtocols(
                "BENEFITS-PROTOCOL-" + uid,
                "benefits-card-detail",
                new ProtocolQueryRequest(
                        uid,
                        h5BenefitsProperties.activate().defaultLoanAmount(),
                        h5LoanProperties.termOptions().isEmpty() ? 0 : h5LoanProperties.termOptions().get(0).value()
                )
        );
        var cardResponse = xiaohuaGatewayService.queryUserCards(
                "BENEFITS-CARD-" + uid,
                "benefits-card-detail",
                new UserCardListRequest(uid)
        );
        List<com.nexusfin.equity.thirdparty.yunka.ProtocolLink> dynamicProtocols = protocolResponse.list() == null
                ? List.of()
                : protocolResponse.list();
        List<BenefitsCardDetailResponse.ProtocolLink> protocols = mergeProtocols(detail.protocols(), dynamicProtocols);
        List<BenefitsCardDetailResponse.UserCard> userCards = cardResponse.cards().stream()
                .map(card -> new BenefitsCardDetailResponse.UserCard(
                        card.cardId(),
                        card.bankName(),
                        card.cardLastFour(),
                        Integer.valueOf(1).equals(card.isDefault())
                ))
                .toList();
        return new BenefitsCardDetailResponse(
                h5I18nService.text("benefits.cardName", detail.cardName()),
                detail.price(),
                detail.totalSaving(),
                mapFeatures(detail.features()),
                mapCategories(detail.categories()),
                mapTips(detail.tips()),
                protocols,
                userCards,
                isProtocolReady(memberId, dynamicProtocols)
        );
    }

    @Override
    public BenefitsActivateResponse activate(String memberId, String uid, BenefitsActivateRequest request) {
        H5BenefitsProperties.Activate activate = h5BenefitsProperties.activate();
        if (!activate.supportedCardType().equals(request.cardType())) {
            throw new BizException(400, "Unsupported card type");
        }
        BenefitsCardDetailResponse cardDetail = getCardDetail(memberId, uid);
        if (!cardDetail.protocolReady()) {
            throw new BizException("BENEFITS_PROTOCOL_NOT_READY", "Benefit activation requires ready agreements and active payment protocol");
        }
        CreateBenefitOrderResponse response = benefitOrderService.createOrder(memberId, new CreateBenefitOrderRequest(
                "activate-" + request.applicationId(),
                h5BenefitsProperties.productCode(),
                activate.defaultLoanAmount(),
                Boolean.TRUE
        ));
        var syncResponse = xiaohuaGatewayService.syncBenefitOrder(
                "BENEFITS-SYNC-" + request.applicationId(),
                response.benefitOrderNo(),
                new BenefitOrderSyncRequest(
                        uid,
                        request.applicationId(),
                        "ACTIVE",
                        activate.defaultLoanAmount()
                )
        );
        if (syncResponse != null && !isBenefitSyncAccepted(syncResponse.status())) {
            throw new BizException("BENEFIT_SYNC_FAILED", syncResponse.message());
        }
        return new BenefitsActivateResponse(
                response.benefitOrderNo(),
                "activated",
                h5I18nService.text("benefits.activate.success", activate.successMessage())
        );
    }

    private List<BenefitsCardDetailResponse.Feature> mapFeatures(List<H5BenefitsProperties.Feature> features) {
        return IntStream.range(0, features.size())
                .mapToObj(index -> {
                    H5BenefitsProperties.Feature feature = features.get(index);
                    return new BenefitsCardDetailResponse.Feature(
                            h5I18nService.text("benefits.feature." + index + ".title", feature.title()),
                            h5I18nService.text("benefits.feature." + index + ".description", feature.description())
                    );
                })
                .toList();
    }

    private List<BenefitsCardDetailResponse.Category> mapCategories(List<H5BenefitsProperties.Category> categories) {
        return categories.stream()
                .map(category -> new BenefitsCardDetailResponse.Category(
                        h5I18nService.text("benefits.category." + category.icon() + ".name", category.name()),
                        category.icon(),
                        mapItems(category.icon(), category.benefits())
                ))
                .toList();
    }

    private List<BenefitsCardDetailResponse.Item> mapItems(String categoryIcon, List<H5BenefitsProperties.Item> benefits) {
        return IntStream.range(0, benefits.size())
                .mapToObj(index -> {
                    H5BenefitsProperties.Item item = benefits.get(index);
                    String keyPrefix = "benefits.item." + categoryIcon + "." + index;
                    return new BenefitsCardDetailResponse.Item(
                            item.discount(),
                            h5I18nService.text(keyPrefix + ".title", item.title()),
                            h5I18nService.text(keyPrefix + ".description", item.description()),
                            h5I18nService.text(keyPrefix + ".validity", item.validity()),
                            item.originalPrice(),
                            item.saving()
                    );
                })
                .toList();
    }

    private List<String> mapTips(List<String> tips) {
        return IntStream.range(0, tips.size())
                .mapToObj(index -> h5I18nService.text("benefits.tip." + index, tips.get(index)))
                .toList();
    }

    private List<BenefitsCardDetailResponse.ProtocolLink> mapProtocols(List<H5BenefitsProperties.ProtocolLink> protocols) {
        return IntStream.range(0, protocols.size())
                .mapToObj(index -> {
                    H5BenefitsProperties.ProtocolLink protocol = protocols.get(index);
                    return new BenefitsCardDetailResponse.ProtocolLink(
                            h5I18nService.text("benefits.protocol." + index + ".name", protocol.name()),
                            protocol.url()
                    );
                })
                .toList();
    }

    private List<BenefitsCardDetailResponse.ProtocolLink> mergeProtocols(
            List<H5BenefitsProperties.ProtocolLink> localProtocols,
            List<com.nexusfin.equity.thirdparty.yunka.ProtocolLink> upstreamProtocols
    ) {
        List<BenefitsCardDetailResponse.ProtocolLink> localized = mapProtocols(localProtocols);
        List<BenefitsCardDetailResponse.ProtocolLink> dynamic = upstreamProtocols == null ? List.of() : upstreamProtocols.stream()
                .filter(protocol -> protocol != null && protocol.url() != null && !protocol.url().isBlank())
                .map(protocol -> new BenefitsCardDetailResponse.ProtocolLink(
                        protocol.title() == null || protocol.title().isBlank() ? "协议" : protocol.title(),
                        protocol.url()
                ))
                .toList();
        return java.util.stream.Stream.concat(localized.stream(), dynamic.stream())
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(
                        protocol -> protocol.url() + "|" + protocol.name(),
                        protocol -> protocol,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    private boolean isProtocolReady(String memberId, List<com.nexusfin.equity.thirdparty.yunka.ProtocolLink> protocols) {
        if (protocols == null || protocols.isEmpty()) {
            return false;
        }
        return memberPaymentProtocolRepository.selectOne(Wrappers.lambdaQuery(
                new com.nexusfin.equity.entity.MemberPaymentProtocol()
        ).eq(com.nexusfin.equity.entity.MemberPaymentProtocol::getMemberId, memberId)
                .eq(com.nexusfin.equity.entity.MemberPaymentProtocol::getProviderCode, "ALLINPAY")
                .eq(com.nexusfin.equity.entity.MemberPaymentProtocol::getProtocolStatus, "ACTIVE")
                .last("limit 1")) != null;
    }

    private boolean isBenefitSyncAccepted(String status) {
        return status == null
                || status.isBlank()
                || "SUCCESS".equalsIgnoreCase(status)
                || "OK".equalsIgnoreCase(status)
                || "0".equals(status)
                || "ACTIVE".equalsIgnoreCase(status);
    }
}
