package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.LoanCalculatorService;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.MoneyUnits.centsToYuan;
import static com.nexusfin.equity.util.MoneyUnits.yuanToCent;

@Service
public class LoanCalculatorServiceImpl implements LoanCalculatorService {

    private final H5LoanProperties h5LoanProperties;
    private final YunkaProperties yunkaProperties;
    private final H5I18nService h5I18nService;
    private final YunkaCallTemplate yunkaCallTemplate;

    public LoanCalculatorServiceImpl(
            H5LoanProperties h5LoanProperties,
            YunkaProperties yunkaProperties,
            H5I18nService h5I18nService,
            YunkaCallTemplate yunkaCallTemplate
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.yunkaProperties = yunkaProperties;
        this.h5I18nService = h5I18nService;
        this.yunkaCallTemplate = yunkaCallTemplate;
    }

    @Override
    public LoanCalculatorConfigResponse getCalculatorConfig() {
        H5LoanProperties.ReceivingAccount receivingAccount = h5LoanProperties.receivingAccount();
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
                new LoanCalculatorConfigResponse.ReceivingAccount(
                        h5I18nService.text("loan.receivingAccount.bankName", receivingAccount.bankName()),
                        receivingAccount.lastFour(),
                        receivingAccount.accountId()
                )
        );
    }

    @Override
    public LoanCalculateResponse calculate(String memberId, String uid, LoanCalculateRequest request) {
        validateCalculateRequest(request);
        String requestId = next("LC");
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan calculate",
                        requestId,
                        yunkaProperties.paths().loanCalculate(),
                        requestId,
                        new LoanTrailForwardData(uid, requestId, yuanToCent(request.amount()), request.term())
                ).withMemberId(memberId)
        );
        long receiveAmount = data.path("receiveAmount").asLong(yuanToCent(request.amount()));
        long repayAmount = data.path("repayAmount").asLong(receiveAmount);
        return new LoanCalculateResponse(
                centsToYuan(repayAmount - receiveAmount),
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

    private void validateCalculateRequest(LoanCalculateRequest request) {
        validateAmountAndTerm(request.amount(), request.term());
    }

    private void validateAmountAndTerm(Long amount, Integer term) {
        H5LoanProperties.AmountRange amountRange = h5LoanProperties.amountRange();
        if (amount < amountRange.min() || amount > amountRange.max()) {
            throw new BizException(400, "amount is out of range");
        }
        if ((amount - amountRange.min()) % amountRange.step() != 0) {
            throw new BizException(400, "amount step is invalid");
        }
        boolean supportedTerm = h5LoanProperties.termOptions().stream()
                .anyMatch(termOption -> termOption.value().equals(term));
        if (!supportedTerm) {
            throw new BizException(400, "term is unsupported");
        }
    }

    private List<LoanCalculateResponse.RepaymentPlanItem> mapRepaymentPlan(JsonNode repaymentPlan) {
        if (!repaymentPlan.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(repaymentPlan.spliterator(), false)
                .map(item -> new LoanCalculateResponse.RepaymentPlanItem(
                        item.path("period").asInt(),
                        item.path("date").asText(),
                        centsToYuan(item.path("principal").asLong()),
                        centsToYuan(item.path("interest").asLong()),
                        centsToYuan(item.path("total").asLong())
                ))
                .toList();
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
            String uid,
            String applyId,
            Long loanAmount,
            Integer loanPeriod
    ) {
    }
}
