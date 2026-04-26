package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncRequest;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncResponse;
import com.nexusfin.equity.thirdparty.yunka.CardSmsConfirmRequest;
import com.nexusfin.equity.thirdparty.yunka.CardSmsConfirmResponse;
import com.nexusfin.equity.thirdparty.yunka.CardSmsSendRequest;
import com.nexusfin.equity.thirdparty.yunka.CardSmsSendResponse;
import com.nexusfin.equity.thirdparty.yunka.CreditImageQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.CreditImageQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.CreditImageSummary;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanItem;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanRequest;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanResponse;
import com.nexusfin.equity.thirdparty.yunka.ProtocolLink;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
import com.nexusfin.equity.thirdparty.yunka.UserQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.UserQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserTokenRequest;
import com.nexusfin.equity.thirdparty.yunka.UserTokenResponse;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class XiaohuaGatewayServiceImpl implements XiaohuaGatewayService {

    private final YunkaGatewayClient yunkaGatewayClient;
    private final YunkaProperties yunkaProperties;
    private final ObjectMapper objectMapper;

    public XiaohuaGatewayServiceImpl(
            YunkaGatewayClient yunkaGatewayClient,
            YunkaProperties yunkaProperties,
            ObjectMapper objectMapper
    ) {
        this.yunkaGatewayClient = yunkaGatewayClient;
        this.yunkaProperties = yunkaProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProtocolQueryResponse queryProtocols(String requestId, String bizOrderNo, ProtocolQueryRequest request) {
        JsonNode data = execute(requestId, yunkaProperties.paths().protocolQuery(), bizOrderNo, request);
        JsonNode listNode = data.isArray() ? data : data.path("list");
        List<ProtocolLink> links = new ArrayList<>();
        if (listNode.isArray()) {
            for (JsonNode item : listNode) {
                links.add(new ProtocolLink(
                        text(item, "title"),
                        integer(item, "isShow"),
                        text(item, "url")
                ));
            }
        }
        return new ProtocolQueryResponse(links);
    }

    @Override
    public UserTokenResponse validateUserToken(String requestId, String bizOrderNo, UserTokenRequest request) {
        JsonNode data = execute(requestId, yunkaProperties.paths().userToken(), bizOrderNo, request);
        return new UserTokenResponse(text(data, "cid"));
    }

    @Override
    public UserQueryResponse queryUser(String requestId, String bizOrderNo, UserQueryRequest request) {
        return new UserQueryResponse(execute(requestId, yunkaProperties.paths().userQuery(), bizOrderNo, request));
    }

    @Override
    public UserCardListResponse queryUserCards(String requestId, String bizOrderNo, UserCardListRequest request) {
        JsonNode data = execute(requestId, yunkaProperties.paths().cardUserCards(), bizOrderNo, request);
        JsonNode listNode = data.path("list");
        if (!listNode.isArray()) {
            listNode = data.path("cards");
        }
        if (!listNode.isArray()) {
            listNode = data.path("cardList");
        }
        List<UserCardSummary> cards = new ArrayList<>();
        if (listNode.isArray()) {
            for (JsonNode item : listNode) {
                String cardId = firstText(item, "cardId", "bankCardNo", "bankCardNum", "cardNo");
                String cardLastFour = firstText(item, "cardLastFour", "cardNoTail", "cardTail");
                if (cardLastFour.isBlank() && !cardId.isBlank() && cardId.length() >= 4) {
                    cardLastFour = cardId.substring(cardId.length() - 4);
                }
                cards.add(new UserCardSummary(
                        cardId,
                        firstText(item, "bankName", "bank", "bankCode", "name"),
                        cardLastFour,
                        firstInteger(item, "isDefault", "defaultFlag")
                ));
            }
        }
        return new UserCardListResponse(cards);
    }

    @Override
    public LoanRepayPlanResponse queryLoanRepayPlan(String requestId, String bizOrderNo, LoanRepayPlanRequest request) {
        JsonNode data = execute(requestId, yunkaProperties.paths().loanRepayPlan(), bizOrderNo, request);
        JsonNode listNode = data.path("repayPlan");
        List<LoanRepayPlanItem> items = new ArrayList<>();
        if (listNode.isArray()) {
            for (JsonNode item : listNode) {
                items.add(new LoanRepayPlanItem(
                        firstInteger(item, "termNo", "period"),
                        firstText(item, "repayDate", "date"),
                        firstLong(item, "repayPrincipal", "principal"),
                        firstLong(item, "repayInterest", "interest"),
                        firstLong(item, "repayAmount", "total")
                ));
            }
        }
        return new LoanRepayPlanResponse(items);
    }

    @Override
    public CardSmsSendResponse sendCardSms(String requestId, String bizOrderNo, CardSmsSendRequest request) {
        JsonNode data = execute(requestId, yunkaProperties.paths().cardSmsSend(), bizOrderNo, request);
        return new CardSmsSendResponse(
                firstText(data, "smsSeq", "requestNo", "serialNo"),
                firstText(data, "status", "result"),
                firstText(data, "msg", "message", "remark")
        );
    }

    @Override
    public CardSmsConfirmResponse confirmCardSms(String requestId, String bizOrderNo, CardSmsConfirmRequest request) {
        JsonNode data = execute(requestId, yunkaProperties.paths().cardSmsConfirm(), bizOrderNo, request);
        return new CardSmsConfirmResponse(
                firstText(data, "status", "result"),
                firstText(data, "msg", "message", "remark")
        );
    }

    @Override
    public CreditImageQueryResponse queryCreditImages(String requestId, String bizOrderNo, CreditImageQueryRequest request) {
        JsonNode data = execute(requestId, yunkaProperties.paths().creditImageQuery(), bizOrderNo, request);
        JsonNode listNode = data.path("list");
        if (!listNode.isArray()) {
            listNode = data.path("images");
        }
        List<CreditImageSummary> images = new ArrayList<>();
        if (listNode.isArray()) {
            for (JsonNode item : listNode) {
                images.add(new CreditImageSummary(
                        firstText(item, "imageType", "type"),
                        firstText(item, "imageUrl", "url")
                ));
            }
        }
        return new CreditImageQueryResponse(images);
    }

    @Override
    public BenefitOrderSyncResponse syncBenefitOrder(String requestId, String bizOrderNo, BenefitOrderSyncRequest request) {
        JsonNode data = execute(requestId, yunkaProperties.paths().benefitSync(), bizOrderNo, request);
        return new BenefitOrderSyncResponse(
                firstText(data, "status", "result"),
                firstText(data, "msg", "message", "remark")
        );
    }

    private JsonNode execute(String requestId, String path, String bizOrderNo, Object payload) {
        YunkaGatewayClient.YunkaGatewayResponse response = yunkaGatewayClient.proxy(
                new YunkaGatewayClient.YunkaGatewayRequest(requestId, path, bizOrderNo, payload)
        );
        if (response == null) {
            throw new BizException(ErrorCodes.YUNKA_RESPONSE_EMPTY, "Yunka gateway response is empty");
        }
        if (response.code() != 0) {
            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, response.message());
        }
        return response.data() == null ? JsonNodeFactory.instance.objectNode() : response.data();
    }

    private String text(JsonNode node, String fieldName) {
        return node.path(fieldName).asText("");
    }

    private Integer integer(JsonNode node, String fieldName) {
        return node.path(fieldName).isMissingNode() ? null : node.path(fieldName).asInt();
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private Integer firstInteger(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asInt();
            }
        }
        return null;
    }

    private Long firstLong(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asLong();
            }
        }
        return null;
    }
}
