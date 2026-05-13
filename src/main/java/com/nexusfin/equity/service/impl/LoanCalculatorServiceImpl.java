package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.LoanCalculatorService;
import com.nexusfin.equity.service.MemberReceivingAccountService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.UserCardListRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.JsonNodes.readDecimal;
import static com.nexusfin.equity.util.LoanInputValidator.validateAmountAndTerm;

@Service
public class LoanCalculatorServiceImpl implements LoanCalculatorService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String USER_CARDS_SOURCE = "YUNKA_USER_CARDS";
    private static final String BIND_CARD_REQUIRED_MESSAGE = "请到科技平台绑卡后重试";

    private final H5LoanProperties h5LoanProperties;
    private final H5BenefitsProperties h5BenefitsProperties;
    private final YunkaProperties yunkaProperties;
    private final H5I18nService h5I18nService;
    private final MemberReceivingAccountService memberReceivingAccountService;
    private final XiaohuaGatewayService xiaohuaGatewayService;
    private final YunkaCallTemplate yunkaCallTemplate;

    public LoanCalculatorServiceImpl(
            H5LoanProperties h5LoanProperties,
            H5BenefitsProperties h5BenefitsProperties,
            YunkaProperties yunkaProperties,
            H5I18nService h5I18nService,
            MemberReceivingAccountService memberReceivingAccountService,
            XiaohuaGatewayService xiaohuaGatewayService,
            YunkaCallTemplate yunkaCallTemplate
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.h5BenefitsProperties = h5BenefitsProperties;
        this.yunkaProperties = yunkaProperties;
        this.h5I18nService = h5I18nService;
        this.memberReceivingAccountService = memberReceivingAccountService;
        this.xiaohuaGatewayService = xiaohuaGatewayService;
        this.yunkaCallTemplate = yunkaCallTemplate;
    }

    @Override
    public LoanCalculatorConfigResponse getCalculatorConfig(String memberId) {
        List<UserCardSummary> userCards = queryUserCards(memberId);
        if (userCards.isEmpty()) {
            return buildCalculatorConfig(null, true, BIND_CARD_REQUIRED_MESSAGE);
        }
        UserCardSummary displayCard = userCards.get(0);
        memberReceivingAccountService.cacheReceivingAccounts(memberId, toCacheCommands(userCards));
        return buildCalculatorConfig(
                new LoanCalculatorConfigResponse.ReceivingAccount(
                        displayCard.bankName(),
                        displayCard.cardLastFour(),
                        displayCard.cardId()
                ),
                false,
                null
        );
    }

    private LoanCalculatorConfigResponse buildCalculatorConfig(
            LoanCalculatorConfigResponse.ReceivingAccount receivingAccount,
            boolean bindCardRequired,
            String bindCardMessage
    ) {
        return new LoanCalculatorConfigResponse(
                new LoanCalculatorConfigResponse.AmountRange(
                        h5LoanProperties.amountRange().min(),
                        h5LoanProperties.amountRange().max(),
                        h5LoanProperties.amountRange().step(),
                        h5LoanProperties.amountRange().defaultAmount()
                ),
                mapTermOptions(h5LoanProperties.termOptions()),
                h5LoanProperties.annualRate(),
                h5I18nService.text("loan.lender", h5LoanProperties.lender()),
                h5BenefitsProperties.detail().price(),
                receivingAccount,
                bindCardRequired,
                bindCardMessage
        );
    }

    @Override
    public LoanCalculateResponse calculate(String memberId, String userId, LoanCalculateRequest request) {
        validateCalculateRequest(request);
        String requestId = next("LC");
        BigDecimal requestedAmount = BigDecimal.valueOf(request.amount()).setScale(2, RoundingMode.UNNECESSARY);
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan calculate",
                        requestId,
                        yunkaProperties.paths().loanCalculate(),
                        requestId,
                        new LoanTrailForwardData(memberId, requestedAmount, request.term())
                ).withMemberId(memberId)
        );
        BigDecimal receiveAmount = readAmountOrDefault(data, requestedAmount, "receiveAmount");
        BigDecimal repayAmount = readAmountOrDefault(data, receiveAmount, "repayAmount");
        BigDecimal feeBaseAmount = readAmountOrDefault(data, receiveAmount, "originalRefund");
        return new LoanCalculateResponse(
                repayAmount.subtract(feeBaseAmount).setScale(2, RoundingMode.HALF_UP),
                readAnnualRate(data),
                mapRepaymentPlan(data.path("repayPlan"))
        );
    }

    private List<LoanCalculatorConfigResponse.TermOption> mapTermOptions(List<H5LoanProperties.TermOption> termOptions) {
        return termOptions.stream()
                .map(termOption -> new LoanCalculatorConfigResponse.TermOption(
                        h5I18nService.text("loan.term." + termOption.value(), termOption.label()),
                        termOption.value()
                ))
                .toList();
    }

    private List<UserCardSummary> queryUserCards(String memberId) {
        String requestId = next("LCC");
        List<UserCardSummary> cards = xiaohuaGatewayService.queryUserCards(
                requestId,
                "calculator-config",
                new UserCardListRequest(memberId)
        ).cards();
        if (cards == null) {
            return List.of();
        }
        for (UserCardSummary card : cards) {
            validateUserCard(card);
        }
        return cards;
    }

    private void validateUserCard(UserCardSummary card) {
        if (card == null
                || !hasText(card.cardId())
                || !hasText(card.bankName())
                || !hasText(card.cardLastFour())) {
            throw new BizException(
                    "USER_CARD_LIST_UNAVAILABLE",
                    "User card list contains unavailable card data"
            );
        }
    }

    private List<MemberReceivingAccountService.CardCacheCommand> toCacheCommands(List<UserCardSummary> cards) {
        java.util.ArrayList<MemberReceivingAccountService.CardCacheCommand> commands = new java.util.ArrayList<>();
        for (int index = 0; index < cards.size(); index++) {
            UserCardSummary card = cards.get(index);
            commands.add(new MemberReceivingAccountService.CardCacheCommand(
                    card.cardId(),
                    card.bankName(),
                    card.cardLastFour(),
                    card.isDefault(),
                    index,
                    USER_CARDS_SOURCE
            ));
        }
        return commands;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateCalculateRequest(LoanCalculateRequest request) {
        validateAmountAndTerm(h5LoanProperties, request.amount(), request.term());
    }

    private List<LoanCalculateResponse.RepaymentPlanItem> mapRepaymentPlan(JsonNode repaymentPlan) {
        if (!repaymentPlan.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(repaymentPlan.spliterator(), false)
                .map(item -> new LoanCalculateResponse.RepaymentPlanItem(
                        readInt(item, "periodNo", "period"),
                        readRepaymentDate(item),
                        readDecimal(item, "repayPrincipal", "principal"),
                        readDecimal(item, "repayInterest", "interest"),
                        readDecimal(item, "repayAmount", "total")
                ))
                .toList();
    }

    private int readInt(JsonNode data, String... fields) {
        for (String field : fields) {
            JsonNode value = data.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                if (value.isInt() || value.isLong()) {
                    return value.asInt();
                }
                String text = value.asText();
                if (!text.isBlank()) {
                    try {
                        return Integer.parseInt(text);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    private String readRepaymentDate(JsonNode data) {
        JsonNode date = data.path("date");
        if (!date.isMissingNode() && !date.isNull() && !date.asText().isBlank()) {
            return date.asText();
        }
        JsonNode dueTime = data.path("dueTime");
        if (dueTime.isMissingNode() || dueTime.isNull()) {
            return "";
        }
        try {
            long epochMillis = dueTime.isNumber() ? dueTime.asLong() : Long.parseLong(dueTime.asText());
            return Instant.ofEpochMilli(epochMillis)
                    .atZone(BUSINESS_ZONE)
                    .toLocalDate()
                    .format(DATE_FORMATTER);
        } catch (NumberFormatException ex) {
            return "";
        }
    }

    private BigDecimal readAmountOrDefault(JsonNode data, BigDecimal fallback, String... fields) {
        BigDecimal value = readDecimal(data, fields);
        return value.compareTo(BigDecimal.ZERO) == 0 ? fallback : value;
    }

    private String readAnnualRate(JsonNode data) {
        JsonNode yearRate = data.path("yearRate");
        if (yearRate.isTextual()) {
            return yearRate.asText();
        }
        if (yearRate.isNumber()) {
            return yearRate.decimalValue().setScale(1, RoundingMode.HALF_UP) + "%";
        }
        return h5LoanProperties.annualRate().multiply(BigDecimal.valueOf(100L))
                .setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private record LoanTrailForwardData(
            String userId,
            BigDecimal loanAmount,
            Integer loanPeriod
    ) {
    }
}
