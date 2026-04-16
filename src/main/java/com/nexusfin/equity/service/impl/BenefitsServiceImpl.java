package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.dto.request.BenefitsActivateRequest;
import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.response.BenefitsActivateResponse;
import com.nexusfin.equity.dto.response.BenefitsCardDetailResponse;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.service.BenefitOrderService;
import com.nexusfin.equity.service.BenefitsService;
import com.nexusfin.equity.service.H5I18nService;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class BenefitsServiceImpl implements BenefitsService {

    private final H5BenefitsProperties h5BenefitsProperties;
    private final BenefitProductRepository benefitProductRepository;
    private final BenefitOrderService benefitOrderService;
    private final H5I18nService h5I18nService;

    public BenefitsServiceImpl(
            H5BenefitsProperties h5BenefitsProperties,
            BenefitProductRepository benefitProductRepository,
            BenefitOrderService benefitOrderService,
            H5I18nService h5I18nService
    ) {
        this.h5BenefitsProperties = h5BenefitsProperties;
        this.benefitProductRepository = benefitProductRepository;
        this.benefitOrderService = benefitOrderService;
        this.h5I18nService = h5I18nService;
    }

    @Override
    public BenefitsCardDetailResponse getCardDetail() {
        BenefitProduct product = benefitProductRepository.selectById(h5BenefitsProperties.productCode());
        if (product == null || !"ACTIVE".equals(product.getStatus())) {
            throw new BizException("PRODUCT_NOT_FOUND", "Benefit product not found");
        }
        H5BenefitsProperties.Detail detail = h5BenefitsProperties.detail();
        return new BenefitsCardDetailResponse(
                h5I18nService.text("benefits.cardName", detail.cardName()),
                detail.price(),
                detail.totalSaving(),
                mapFeatures(detail.features()),
                mapCategories(detail.categories()),
                mapTips(detail.tips()),
                mapProtocols(detail.protocols())
        );
    }

    @Override
    public BenefitsActivateResponse activate(String memberId, BenefitsActivateRequest request) {
        H5BenefitsProperties.Activate activate = h5BenefitsProperties.activate();
        if (!activate.supportedCardType().equals(request.cardType())) {
            throw new BizException(400, "Unsupported card type");
        }
        CreateBenefitOrderResponse response = benefitOrderService.createOrder(memberId, new CreateBenefitOrderRequest(
                "activate-" + request.applicationId(),
                h5BenefitsProperties.productCode(),
                activate.defaultLoanAmount(),
                Boolean.TRUE
        ));
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
}
