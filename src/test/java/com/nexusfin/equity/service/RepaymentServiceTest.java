package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.H5RepaymentProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.RepaymentSmsConfirmRequest;
import com.nexusfin.equity.dto.request.RepaymentSmsSendRequest;
import com.nexusfin.equity.dto.response.RepaymentInfoResponse;
import com.nexusfin.equity.dto.response.RepaymentResultResponse;
import com.nexusfin.equity.dto.response.RepaymentSmsConfirmResponse;
import com.nexusfin.equity.dto.response.RepaymentSmsSendResponse;
import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.dto.request.RepaymentSubmitRequest;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.RepaymentServiceImpl;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient.YunkaGatewayRequest;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.CardSmsConfirmResponse;
import com.nexusfin.equity.thirdparty.yunka.CardSmsSendResponse;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanItem;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.SensitiveDataCipher;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private YunkaGatewayClient yunkaGatewayClient;

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private XiaohuaGatewayService xiaohuaGatewayService;

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private SensitiveDataCipher sensitiveDataCipher;

    @Mock
    private MemberReceivingAccountService memberReceivingAccountService;

    private RepaymentService repaymentService;

    @BeforeEach
    void setUp() {
        lenient().when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(memberReceivingAccountService.getDefaultReceivingAccount("mem-test-001"))
                .thenReturn(new MemberReceivingAccountService.ReceivingAccountDetails("acc_001", "招商银行", "8648"));
        lenient().when(memberInfoRepository.selectById("mem-test-001")).thenReturn(memberInfo());
        lenient().when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        lenient().when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        lenient().when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        repaymentService = repaymentService(false);
    }

    private RepaymentService repaymentService(boolean smsRequired) {
        return new RepaymentServiceImpl(
                h5LoanProperties(),
                new H5RepaymentProperties(smsRequired),
                yunkaProperties(),
                yunkaGatewayClient,
                h5I18nService,
                xiaohuaGatewayService,
                memberInfoRepository,
                loanApplicationMappingRepository,
                idempotencyRecordRepository,
                sensitiveDataCipher,
                memberReceivingAccountService,
                new YunkaCallTemplate(yunkaGatewayClient)
        );
    }

    @Test
    void shouldReturnBoundCardsAndDefaultCardInRepaymentInfo() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260501));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260501"), any()))
                .thenReturn(repayPlanWithDuePeriods());
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {
                          "status": 5004,
                          "remark": "试算成功，请确认还款",
                          "repayAmount": 1018.50,
                          "repayPrincipal": 1000.00,
                          "repayInterest": 18.50,
                          "repayPenaltyInt": 0,
                          "repayBreakFee": 0,
                          "repayOtherCharge": 0,
                          "repaySvcFee": 0,
                          "repayGuaranteeFee": 0,
                          "discount": 26.50,
                          "originalRepay": 1045.00
                        }
                        """)
        ));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("20260501"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1),
                        new UserCardSummary("card-002", "建设银行", "1234", 0)
                )));

        RepaymentInfoResponse response = repaymentService.getInfo("mem-test-001", 20260501);

        assertThat(response.bankCard().bankName()).isEqualTo("招商银行");
        assertThat(response.bankCard().accountId()).isEqualTo("card-001");
        assertThat(response.bankCards()).hasSize(2);
        assertThat(response.smsRequired()).isFalse();
        assertThat(response.remark()).isEqualTo("试算成功，请确认还款");
        assertThat(response.fees().repayPrincipal()).isEqualByComparingTo("1000.00");
        assertThat(response.fees().repayInterest()).isEqualByComparingTo("18.50");
        assertThat(response.fees().discount()).isEqualByComparingTo("26.50");
        assertThat(response.fees().originalRepay()).isEqualByComparingTo("1045.00");
        assertThat(response.tip()).isEqualTo("试算成功，请确认还款");

        ArgumentCaptor<YunkaGatewayRequest> captor = ArgumentCaptor.forClass(YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        JsonNode payload = objectMapper.valueToTree(captor.getValue().data());
        assertThat(captor.getValue().path()).isEqualTo("/repay/trial");
        assertThat(payload.path("userId").asText()).isEqualTo("mem-test-001");
        assertThat(payload.path("loanId").isInt()).isTrue();
        assertThat(payload.path("loanId").asInt()).isEqualTo(20260501);
        assertThat(payload.path("repayType").isInt()).isTrue();
        assertThat(payload.path("repayType").asInt()).isEqualTo(2);
        assertThat(payload.path("periods").asText()).isEqualTo("2");
        assertThat(payload.path("userId").asText()).isNotEqualTo("cid-test-001");
        assertThat(payload.has("uid")).isFalse();
    }

    @Test
    void shouldReturnSmsRequiredWhenRepaymentSmsIsEnabledByConfig() throws Exception {
        repaymentService = repaymentService(true);
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260501));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260501"), any()))
                .thenReturn(repayPlanWithDuePeriods());
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {"status":5004,"repayAmount":1018.50}
                        """)
        ));

        RepaymentInfoResponse response = repaymentService.getInfo("mem-test-001", 20260501);

        assertThat(response.smsRequired()).isTrue();
    }

    @Test
    void shouldUseEarliestOverdueOrUnpaidPeriodWhenResolvingRepaymentInfo() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260509));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260509"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(3, 3, 1, "2026-07-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(2, 2, 3, "2026-06-07", 100000L, 3000L, 103000L)
                )));
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {"status":5004,"repayAmount":1018.50}
                        """)
        ));

        repaymentService.getInfo("mem-test-001", 20260509);

        ArgumentCaptor<YunkaGatewayRequest> captor = ArgumentCaptor.forClass(YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        JsonNode payload = objectMapper.valueToTree(captor.getValue().data());
        assertThat(payload.path("periods").asText()).isEqualTo("2");
    }

    @Test
    void shouldRejectRepaymentInfoWhenRepayPlanHasNoDuePeriods() {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260501));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260501"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, 1, 2, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, 2, 2, "2026-06-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(3, null, 1, "2026-07-07", 100000L, 3000L, 103000L)
                )));

        assertThatThrownBy(() -> repaymentService.getInfo("mem-test-001", 20260501))
                .isInstanceOf(BizException.class)
                .extracting(
                        throwable -> ((BizException) throwable).getErrorNo(),
                        throwable -> ((BizException) throwable).getErrorMsg()
                )
                .containsExactly(
                        "REPAYMENT_REPAY_PLAN_UNAVAILABLE",
                        "No current due periods found for repayment"
                );

        verifyNoInteractions(yunkaGatewayClient);
    }

    @Test
    void shouldRejectUnknownLoanIdBeforeCallingYunka() {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> repaymentService.getInfo("mem-test-001", 99999901))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getCode(),
                        throwable -> ((BizException) throwable).getErrorMsg())
                .containsExactly(404, "repayment loan reference not found");

        verifyNoInteractions(yunkaGatewayClient);
        verify(xiaohuaGatewayService, never()).queryUserCards(any(), any(), any());
    }

    @Test
    void shouldSendRepaymentSmsUsingSelectedCardAndMemberProfile() {
        when(xiaohuaGatewayService.queryUserCards(any(), eq("20260502"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));
        when(memberInfoRepository.selectById("mem-test-001")).thenReturn(memberInfo());
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(xiaohuaGatewayService.sendCardSms(any(), eq("20260502"), any()))
                .thenReturn(new CardSmsSendResponse("sms-001", "11001", "发送成功"));

        RepaymentSmsSendResponse response = repaymentService.sendSms(
                "mem-test-001",
                new RepaymentSmsSendRequest(20260502, "card-001")
        );

        assertThat(response.smsSeq()).isEqualTo("sms-001");
        assertThat(response.status()).isEqualTo("sent");
        ArgumentCaptor<com.nexusfin.equity.thirdparty.yunka.CardSmsSendRequest> captor =
                ArgumentCaptor.forClass(com.nexusfin.equity.thirdparty.yunka.CardSmsSendRequest.class);
        verify(xiaohuaGatewayService).sendCardSms(any(), eq("20260502"), captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("mem-test-001");
        assertThat(captor.getValue().loanId()).isEqualTo(20260502);
        assertThat(captor.getValue().bankCardNum()).isEqualTo("card-001");
        assertThat(captor.getValue().type()).isEqualTo(2);
        assertThat(captor.getValue().phone()).isEqualTo("13800138000");
    }

    @Test
    void shouldConfirmRepaymentSmsUsingLatestGatewayFields() {
        when(memberInfoRepository.selectById("mem-test-001")).thenReturn(memberInfo());
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(xiaohuaGatewayService.confirmCardSms(any(), eq("20260503"), any()))
                .thenReturn(new CardSmsConfirmResponse("11002", "验证成功"));

        RepaymentSmsConfirmResponse response = repaymentService.confirmSms(
                "mem-test-001",
                new RepaymentSmsConfirmRequest(20260503, "123456")
        );

        assertThat(response.status()).isEqualTo("confirmed");
    }

    @Test
    void shouldMapLatestRepaymentQueryStatuses() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260501));
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {
                          "status": "8004",
                          "amount": 1018.50,
                          "swiftNumber": "RP-20260501",
                          "discount": 26.50,
                          "bankCardNum": "6222020202028648"
                        }
                        """)
        ));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("RP-20260501"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("6222020202028648", "招商银行", "8648", 1)
                )));

        RepaymentResultResponse response = repaymentService.getResult("mem-test-001", "RP-20260501");

        assertThat(response.status()).isEqualTo("processing");
        assertThat(response.swiftNumber()).isEqualTo("RP-20260501");
        assertThat(response.remark()).isEmpty();
        assertThat(response.interestSaved()).isEqualByComparingTo("26.50");
        assertThat(response.bankCard().lastFour()).isEqualTo("8648");

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> captor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        JsonNode data = objectMapper.valueToTree(captor.getValue().data());
        assertThat(captor.getValue().path()).isEqualTo("/repay/query");
        assertThat(data.get("userId").asText()).isEqualTo("mem-test-001");
        assertThat(data.get("userId").asText()).isNotEqualTo("cid-test-001");
        assertThat(data.has("uid")).isFalse();
        assertThat(data.get("loanId").isInt()).isTrue();
        assertThat(data.get("loanId").asInt()).isEqualTo(20260501);
        assertThat(data.get("swiftNumber").asText()).isEqualTo("RP-20260501");
    }

    @Test
    void shouldQueryRepaymentResultUsingStoredSwiftNumberReference() throws Exception {
        String swiftNumber = "xhqbapi20260529163815470019";
        IdempotencyRecord repaymentReference = new IdempotencyRecord();
        repaymentReference.setResponseBody("20260501");
        when(idempotencyRecordRepository.selectById(any())).thenReturn(repaymentReference);
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260501));
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {
                          "status": "8004",
                          "amount": 1018.50,
                          "swiftNumber": "xhqbapi20260529163815470019",
                          "remark": "还款已受理"
                        }
                        """)
        ));

        RepaymentResultResponse response = repaymentService.getResult("mem-test-001", swiftNumber);

        assertThat(response.repaymentId()).isEqualTo(swiftNumber);
        assertThat(response.swiftNumber()).isEqualTo(swiftNumber);
        assertThat(response.status()).isEqualTo("processing");
        assertThat(response.remark()).isEqualTo("还款已受理");

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> captor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        JsonNode data = objectMapper.valueToTree(captor.getValue().data());
        assertThat(captor.getValue().path()).isEqualTo("/repay/query");
        assertThat(data.get("userId").asText()).isEqualTo("mem-test-001");
        assertThat(data.get("loanId").asInt()).isEqualTo(20260501);
        assertThat(data.get("swiftNumber").asText()).isEqualTo(swiftNumber);
    }

    @Test
    void shouldRejectUnknownRepaymentIdBeforeCallingYunka() {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> repaymentService.getResult("mem-test-001", "RP-99999901"))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getCode(),
                        throwable -> ((BizException) throwable).getErrorMsg())
                .containsExactly(404, "repayment reference not found");

        verifyNoInteractions(yunkaGatewayClient);
        verify(xiaohuaGatewayService, never()).queryUserCards(any(), any(), any());
    }

    @Test
    void shouldTranslateRepaymentSubmitTimeoutToBizException() {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260504));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260504"), any()))
                .thenReturn(repayPlanWithDuePeriods());
        when(yunkaGatewayClient.proxy(any()))
                .thenAnswer(invocation -> {
                    YunkaGatewayRequest gatewayRequest = invocation.getArgument(0);
                    if ("/repay/trial".equals(gatewayRequest.path())) {
                        return new YunkaGatewayClient.YunkaGatewayResponse(
                                0,
                                "SUCCESS",
                                objectMapper.readTree("""
                                        {"repayAmount":1018.50}
                                        """)
                        );
                    }
                    throw new UpstreamTimeoutException("Yunka gateway timeout");
                });

        assertThatThrownBy(() -> repaymentService.submit(
                "mem-test-001",
                new RepaymentSubmitRequest(20260504, BigDecimal.valueOf(1018.50), "acc_001", "early")
        ))
                .isInstanceOf(BizException.class)
                .extracting(
                        throwable -> ((BizException) throwable).getErrorNo(),
                        throwable -> ((BizException) throwable).getErrorMsg()
                )
                .containsExactly(
                        ErrorCodes.YUNKA_UPSTREAM_TIMEOUT,
                        "Repayment submit temporarily unavailable"
                );
    }

    @Test
    void shouldRejectRepaymentSubmitWhenAmountExceedsCurrentRepayableAmount() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260505));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260505"), any()))
                .thenReturn(repayPlanWithDuePeriods());
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {"repayAmount":1018.50}
                        """)
        ));

        assertThatThrownBy(() -> repaymentService.submit(
                "mem-test-001",
                new RepaymentSubmitRequest(20260505, BigDecimal.valueOf(2000.00), "acc_001", "early")
        ))
                .isInstanceOf(BizException.class)
                .extracting(
                        throwable -> ((BizException) throwable).getErrorNo(),
                        throwable -> ((BizException) throwable).getErrorMsg()
                )
                .containsExactly(
                        "REPAYMENT_AMOUNT_EXCEEDED",
                        "Repayment amount exceeds current repayable amount"
                );

        ArgumentCaptor<YunkaGatewayRequest> captor = ArgumentCaptor.forClass(YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(captor.capture());
        assertThat(captor.getValue().path()).isEqualTo("/repay/trial");
    }

    @Test
    void shouldRejectDuplicateRepaymentSubmitBeforeSecondRepayApplyCall() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260506));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260506"), any()))
                .thenReturn(repayPlanWithDuePeriods());
        when(yunkaGatewayClient.proxy(any()))
                .thenAnswer(invocation -> {
                    YunkaGatewayRequest gatewayRequest = invocation.getArgument(0);
                    if ("/repay/trial".equals(gatewayRequest.path())) {
                        return new YunkaGatewayClient.YunkaGatewayResponse(
                                0,
                                "SUCCESS",
                                objectMapper.readTree("""
                                        {"repayAmount":1018.50}
                                        """)
                        );
                    }
                    return new YunkaGatewayClient.YunkaGatewayResponse(
                            0,
                            "SUCCESS",
                            objectMapper.readTree("""
                                    {"swiftNumber":"RP-20260506","status":"8004","remark":"processing"}
                                    """)
                    );
                });
        when(idempotencyRecordRepository.insert(any()))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("duplicate repayment submit"));

        repaymentService.submit(
                "mem-test-001",
                new RepaymentSubmitRequest(20260506, BigDecimal.valueOf(1018.50), "acc_001", "early")
        );

        assertThatThrownBy(() -> repaymentService.submit(
                "mem-test-001",
                new RepaymentSubmitRequest(20260506, BigDecimal.valueOf(1018.50), "acc_001", "early")
        ))
                .isInstanceOf(BizException.class)
                .extracting(
                        throwable -> ((BizException) throwable).getErrorNo(),
                        throwable -> ((BizException) throwable).getErrorMsg()
                )
                .containsExactly(
                        "REPAYMENT_SUBMIT_DUPLICATED",
                        "Repayment request is duplicated"
                );

        ArgumentCaptor<YunkaGatewayRequest> captor = ArgumentCaptor.forClass(YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, times(3)).proxy(captor.capture());
        assertThat(captor.getAllValues().stream()
                .filter(request -> "/repay/apply".equals(request.path())))
                .hasSize(1);
    }

    @Test
    void shouldSendRepaymentAmountToYunkaInYuanAtBoundary() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260507));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260507"), any()))
                .thenReturn(repayPlanWithDuePeriods());
        when(memberInfoRepository.selectById("mem-test-001")).thenReturn(memberInfo());
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(yunkaGatewayClient.proxy(any()))
                .thenAnswer(invocation -> {
                    YunkaGatewayRequest gatewayRequest = invocation.getArgument(0);
                    if ("/repay/trial".equals(gatewayRequest.path())) {
                        return new YunkaGatewayClient.YunkaGatewayResponse(
                                0,
                                "SUCCESS",
                                objectMapper.readTree("""
                                        {"repayAmount":1018.50}
                                        """)
                        );
                    }
                    return new YunkaGatewayClient.YunkaGatewayResponse(
                            0,
                            "SUCCESS",
                            objectMapper.readTree("""
                                    {"swiftNumber":"RP-20260507","status":"8004","remark":"processing"}
                                    """)
                    );
                });
        when(idempotencyRecordRepository.insert(any())).thenReturn(1);

        repaymentService.submit(
                "mem-test-001",
                new RepaymentSubmitRequest(20260507, BigDecimal.valueOf(1018.50), "acc_001", "scheduled")
        );

        ArgumentCaptor<YunkaGatewayRequest> captor = ArgumentCaptor.forClass(YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, times(2)).proxy(captor.capture());
        YunkaGatewayRequest repayApplyRequest = captor.getAllValues().stream()
                .filter(request -> "/repay/apply".equals(request.path()))
                .findFirst()
                .orElseThrow();
        JsonNode repayApplyPayload = objectMapper.valueToTree(repayApplyRequest.data());
        YunkaGatewayRequest repayTrialRequest = captor.getAllValues().stream()
                .filter(request -> "/repay/trial".equals(request.path()))
                .findFirst()
                .orElseThrow();
        JsonNode repayTrialPayload = objectMapper.valueToTree(repayTrialRequest.data());
        assertThat(repayTrialPayload.path("userId").asText()).isEqualTo("mem-test-001");
        assertThat(repayTrialPayload.path("loanId").isInt()).isTrue();
        assertThat(repayTrialPayload.path("loanId").asInt()).isEqualTo(20260507);
        assertThat(repayTrialPayload.path("periods").asText()).isEqualTo("2");
        assertThat(repayTrialPayload.path("userId").asText()).isNotEqualTo("cid-test-001");
        assertThat(repayTrialPayload.has("uid")).isFalse();
        assertThat(repayApplyPayload.path("userId").asText()).isEqualTo("mem-test-001");
        assertThat(repayApplyPayload.path("loanId").isInt()).isTrue();
        assertThat(repayApplyPayload.path("loanId").asInt()).isEqualTo(20260507);
        assertThat(repayApplyPayload.path("repayType").asInt()).isEqualTo(2);
        assertThat(repayApplyPayload.path("periods").asText()).isEqualTo("2");
        assertThat(repayApplyPayload.path("bankCardNum").asText()).isEqualTo("acc_001");
        assertThat(repayApplyPayload.has("bankCardNo")).isFalse();
        assertThat(repayApplyPayload.path("phone").asText()).isEqualTo("13800138000");
        assertThat(repayApplyPayload.path("cid").asText()).isEqualTo("cid-test-001");
        assertThat(repayApplyPayload.path("idno").asText()).isEqualTo("110101199003071234");
        assertThat(repayApplyPayload.path("name").asText()).isEqualTo("测试用户");
        assertThat(repayApplyPayload.path("userId").asText()).isNotEqualTo("cid-test-001");
        assertThat(repayApplyPayload.has("uid")).isFalse();
        assertThat(repayApplyPayload.path("repayAmount").decimalValue()).isEqualByComparingTo("1018.50");
    }

    @Test
    void shouldMapRepayApplyFailureStatusToFailed() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260508));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260508"), any()))
                .thenReturn(repayPlanWithDuePeriods());
        when(memberInfoRepository.selectById("mem-test-001")).thenReturn(memberInfo());
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(yunkaGatewayClient.proxy(any()))
                .thenAnswer(invocation -> {
                    YunkaGatewayRequest gatewayRequest = invocation.getArgument(0);
                    if ("/repay/trial".equals(gatewayRequest.path())) {
                        return new YunkaGatewayClient.YunkaGatewayResponse(
                                0,
                                "SUCCESS",
                                objectMapper.readTree("""
                                        {"repayAmount":1018.50}
                                        """)
                        );
                    }
                    return new YunkaGatewayClient.YunkaGatewayResponse(
                            0,
                            "SUCCESS",
                            objectMapper.readTree("""
                                    {"status":"5002","remark":"还款请求处理失败"}
                                    """)
                    );
                });
        when(idempotencyRecordRepository.insert(any())).thenReturn(1);

        var response = repaymentService.submit(
                "mem-test-001",
                new RepaymentSubmitRequest(20260508, BigDecimal.valueOf(1018.50), "acc_001", "scheduled")
        );

        assertThat(response.status()).isEqualTo("failed");
        assertThat(response.message()).isEqualTo("还款请求处理失败");
    }

    @Test
    void shouldPersistSwiftNumberReferenceAfterRepayApplyAccepted() throws Exception {
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(loanMapping("mem-test-001", "cid-test-001", 20260510));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260510"), any()))
                .thenReturn(repayPlanWithDuePeriods());
        when(memberInfoRepository.selectById("mem-test-001")).thenReturn(memberInfo());
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(yunkaGatewayClient.proxy(any()))
                .thenAnswer(invocation -> {
                    YunkaGatewayRequest gatewayRequest = invocation.getArgument(0);
                    if ("/repay/trial".equals(gatewayRequest.path())) {
                        return new YunkaGatewayClient.YunkaGatewayResponse(
                                0,
                                "SUCCESS",
                                objectMapper.readTree("""
                                        {"repayAmount":1018.50}
                                        """)
                        );
                    }
                    return new YunkaGatewayClient.YunkaGatewayResponse(
                            0,
                            "SUCCESS",
                            objectMapper.readTree("""
                                    {
                                      "swiftNumber":"xhqbapi20260529163815470019",
                                      "status":"5001",
                                      "remark":"还款已受理"
                                    }
                                    """)
                    );
                });
        when(idempotencyRecordRepository.insert(any())).thenReturn(1);

        var response = repaymentService.submit(
                "mem-test-001",
                new RepaymentSubmitRequest(20260510, BigDecimal.valueOf(1018.50), "acc_001", "scheduled")
        );

        assertThat(response.repaymentId()).isEqualTo("xhqbapi20260529163815470019");

        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository, times(2)).insert(captor.capture());
        IdempotencyRecord reference = captor.getAllValues().stream()
                .filter(record -> "REPAYMENT_RESULT".equals(record.getBizType()))
                .findFirst()
                .orElseThrow();
        assertThat(reference.getBizKey()).isEqualTo("mem-test-001:xhqbapi20260529163815470019");
        assertThat(reference.getResponseBody()).isEqualTo("20260510");
    }

    private LoanRepayPlanResponse repayPlanWithDuePeriods() {
        return new LoanRepayPlanResponse(List.of(
                new LoanRepayPlanItem(3, 3, 1, "2026-07-07", 100000L, 3000L, 103000L),
                new LoanRepayPlanItem(1, 1, 2, "2026-05-07", 100000L, 4500L, 104500L),
                new LoanRepayPlanItem(2, 2, 2, "2026-06-07", 100000L, 3000L, 103000L),
                new LoanRepayPlanItem(2, 2, 1, "2026-06-07", 100000L, 3000L, 103000L)
        ));
    }

    private LoanApplicationMapping loanMapping(String memberId, String externalUserId, Integer loanId) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-" + loanId);
        mapping.setMemberId(memberId);
        mapping.setExternalUserId(externalUserId);
        mapping.setPlatformLoanId(loanId);
        mapping.setMappingStatus("ACTIVE");
        return mapping;
    }

    private MemberInfo memberInfo() {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId("mem-test-001");
        memberInfo.setTechPlatformUserId("cid-test-001");
        memberInfo.setExternalUserId("cid-test-001");
        memberInfo.setMobileEncrypted("mobile-cipher");
        memberInfo.setIdCardEncrypted("id-cipher");
        memberInfo.setRealNameEncrypted("name-cipher");
        return memberInfo;
    }

    private H5LoanProperties h5LoanProperties() {
        return new H5LoanProperties(
                new H5LoanProperties.AmountRange(100L, 5000L, 100L, 3000L),
                List.of(new H5LoanProperties.TermOption("3期", 3)),
                BigDecimal.valueOf(0.18),
                "XX商业银行"
        );
    }

    private YunkaProperties yunkaProperties() {
        return new YunkaProperties(
                true,
                "REST",
                "http://localhost:8080",
                "/api/gateway/proxy",
                3000,
                5000,
                new YunkaProperties.Paths(
                        "/loan/trial",
                        "/loan/query",
                        "/loan/apply",
                        "/repay/trial",
                        "/repay/apply",
                        "/repay/query",
                        "/protocol/queryProtocolAggregationLink",
                        "/user/token",
                        "/user/query",
                        "/loan/repayPlan",
                        "/card/smsSend",
                        "/card/smsConfirm",
                        "/card/userCards",
                        "/credit/image/query",
                        "/vip/orderNotice"
                ),
                "ABS",
                "ABS-YUNKA-TEST",
                "yunka-test-secret"
        );
    }
}
