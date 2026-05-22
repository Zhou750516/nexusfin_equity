package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.service.impl.LoanApplicationServiceImpl;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class LoanApplicationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private YunkaGatewayClient yunkaGatewayClient;

    @Mock
    private LoanApplicationGateway loanApplicationGateway;

    @Mock
    private BenefitOrderService benefitOrderService;

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private MemberReceivingAccountService memberReceivingAccountService;

    @Mock
    private AsyncCompensationEnqueueService asyncCompensationEnqueueService;

    private LoanApplicationService loanApplicationService;

    @BeforeEach
    void setUp() {
        lenient().when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(memberReceivingAccountService.getReceivingAccount("mem-test-001", "acc_001"))
                .thenReturn(new MemberReceivingAccountService.ReceivingAccountDetails("acc_001", "招商银行", "8648"));
        loanApplicationService = new LoanApplicationServiceImpl(
                h5LoanProperties(),
                h5BenefitsProperties(),
                yunkaProperties(),
                loanApplicationGateway,
                benefitOrderService,
                h5I18nService,
                memberReceivingAccountService,
                asyncCompensationEnqueueService,
                new YunkaCallTemplate(yunkaGatewayClient)
        );
    }

    @Test
    void shouldForwardRichApplyFieldsAndCreateActiveMappingOnSuccessfulLoanApply() throws Exception {
        when(loanApplicationGateway.findLatestPendingMapping("mem-test-001")).thenReturn(null);
        when(benefitOrderService.createLocalOrder(eq("mem-test-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-001", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(successfulUserQueryResponse())
                .thenReturn(successfulImageQueryResponse())
                .thenReturn(successfulLoanApplyResponse(20260521));

        LoanApplyResponse response = loanApplicationService.apply("mem-test-001", "cid-test-001",
                buildApplyRequestWithSparseDocumentFields("PBEN-SHOULD-NOT-BE-FORWARDED"));

        assertThat(response.applicationId()).startsWith("APP-");
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.benefitOrderNo()).isEqualTo("BEN-001");
        assertThat(response.message()).isEqualTo("处理中");

        ArgumentCaptor<com.nexusfin.equity.dto.request.CreateBenefitOrderRequest> benefitOrderCaptor =
                ArgumentCaptor.forClass(com.nexusfin.equity.dto.request.CreateBenefitOrderRequest.class);
        verify(benefitOrderService).createLocalOrder(eq("mem-test-001"), benefitOrderCaptor.capture());
        assertThat(benefitOrderCaptor.getValue().loanAmount()).isEqualTo(29900L);
        assertThat(benefitOrderCaptor.getValue().benefitAmount()).isNull();
        verify(benefitOrderService, never()).createOrder(any(), any());

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> yunkaCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, org.mockito.Mockito.times(3)).proxy(yunkaCaptor.capture());
        assertThat(yunkaCaptor.getAllValues()).hasSize(3);
        assertThat(yunkaCaptor.getAllValues().get(0).path()).isEqualTo("/user/query");
        assertThat(yunkaCaptor.getAllValues().get(1).path()).isEqualTo("/credit/image/query");
        assertThat(yunkaCaptor.getAllValues().get(2).path()).isEqualTo("/loan/apply");
        JsonNode userQueryData = objectMapper.valueToTree(yunkaCaptor.getAllValues().get(0).data());
        assertThat(userQueryData.path("userId").asText()).isEqualTo("mem-test-001");
        assertThat(userQueryData.path("cid").asText()).isEqualTo("cid-test-001");
        JsonNode imageQueryData = objectMapper.valueToTree(yunkaCaptor.getAllValues().get(1).data());
        assertThat(imageQueryData.path("userId").asText()).isEqualTo("mem-test-001");
        assertThat(imageQueryData.path("type").asText()).isEqualTo("back,front,nature");

        JsonNode forwardData = objectMapper.valueToTree(yunkaCaptor.getAllValues().get(2).data());
        assertThat(forwardData.path("userId").asText()).isEqualTo("mem-test-001");
        assertThat(forwardData.path("userId").asText()).isNotEqualTo("cid-test-001");
        assertThat(forwardData.has("uid")).isFalse();
        assertThat(forwardData.has("purpose")).isFalse();
        assertThat(forwardData.path("loanReason").asText()).isEqualTo("70006");
        assertThat(forwardData.path("bankCardNum").asText()).isEqualTo("6222020202028648");
        assertThat(forwardData.path("platformBenefitOrderNo").asText()).isEqualTo(response.applicationId());
        assertThat(forwardData.path("loanId").isInt()).isTrue();
        assertThat(forwardData.path("loanId").asInt()).isPositive();
        assertThat(forwardData.path("loanAmount").decimalValue()).isEqualByComparingTo("3000.00");
        assertThat(forwardData.path("phone").asText()).isEqualTo("13800138000");
        assertThat(forwardData.path("idno").asText()).isEqualTo("310101199001011111");
        assertThat(forwardData.path("name").asText()).isEqualTo("张三");
        assertThat(forwardData.path("basicInfo").path("monthlyIncome").asInt()).isEqualTo(10000);
        assertThat(forwardData.path("idInfo").path("idno").asText()).isEqualTo("310101199001011111");
        assertThat(forwardData.path("contactInfo").isArray()).isTrue();
        assertThat(forwardData.path("supplementInfo").path("occupation").asText()).isEqualTo("20001");
        assertThat(forwardData.path("optionInfo").path("maritalStatus").asText()).isEqualTo("50002");
        assertThat(forwardData.path("imageInfo").isArray()).isTrue();
        assertThat(forwardData.path("imageInfo").get(0).path("back").asText()).isEqualTo("BACK_BASE64_LONG_VALUE");
        assertThat(forwardData.path("imageInfo").get(0).path("front").asText()).isEqualTo("FRONT_BASE64_LONG_VALUE");
        assertThat(forwardData.path("imageInfo").get(0).path("nature").asText()).isEqualTo("NATURE_BASE64_LONG_VALUE");
        assertThat(forwardData.path("imageInfo").get(0).path("type").asText()).isEqualTo("back,front,nature");

        ArgumentCaptor<LoanApplicationGateway.SaveCommand> saveCaptor =
                ArgumentCaptor.forClass(LoanApplicationGateway.SaveCommand.class);
        verify(loanApplicationGateway).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().memberId()).isEqualTo("mem-test-001");
        assertThat(saveCaptor.getValue().externalUserId()).isEqualTo("cid-test-001");
        assertThat(saveCaptor.getValue().applicationId()).isEqualTo(response.applicationId());
        assertThat(saveCaptor.getValue().benefitOrderNo()).isEqualTo("BEN-001");
        assertThat(saveCaptor.getValue().platformLoanId()).isEqualTo(20260521);
        assertThat(saveCaptor.getValue().purpose()).isEqualTo("shopping");
        assertThat(saveCaptor.getValue().mappingStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldFillPlatformBenefitOrderNoFromGeneratedApplicationIdWhenRequestValueIsMissing() throws Exception {
        when(loanApplicationGateway.findLatestPendingMapping("mem-test-001")).thenReturn(null);
        when(benefitOrderService.createLocalOrder(eq("mem-test-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-NO-PBO", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(successfulUserQueryResponse())
                .thenReturn(successfulImageQueryResponse())
                .thenReturn(successfulLoanApplyResponse(20260522));

        LoanApplyResponse response = loanApplicationService.apply(
                "mem-test-001",
                "cid-test-001",
                buildApplyRequestWithSparseDocumentFields(null)
        );

        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.benefitOrderNo()).isEqualTo("BEN-NO-PBO");

        verify(benefitOrderService).createLocalOrder(eq("mem-test-001"), any());
        verify(benefitOrderService, never()).createOrder(any(), any());
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> yunkaCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, org.mockito.Mockito.times(3)).proxy(yunkaCaptor.capture());
        JsonNode forwardData = objectMapper.valueToTree(yunkaCaptor.getAllValues().get(2).data());
        assertThat(forwardData.path("userId").asText()).isEqualTo("mem-test-001");
        assertThat(forwardData.has("platformBenefitOrderNo")).isTrue();
        assertThat(forwardData.path("platformBenefitOrderNo").asText()).isEqualTo(response.applicationId());
        assertThat(forwardData.path("applyId").asText()).isEqualTo(response.applicationId());

        ArgumentCaptor<LoanApplicationGateway.SaveCommand> saveCaptor =
                ArgumentCaptor.forClass(LoanApplicationGateway.SaveCommand.class);
        verify(loanApplicationGateway).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().benefitOrderNo()).isEqualTo("BEN-NO-PBO");
        assertThat(saveCaptor.getValue().platformLoanId()).isEqualTo(20260522);
    }

    @Test
    void shouldReturnFailedResponseWhenLoanApplyIsRejected(CapturedOutput output) throws Exception {
        when(loanApplicationGateway.findLatestPendingMapping("mem-test-001")).thenReturn(null);
        when(benefitOrderService.createLocalOrder(eq("mem-test-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-REJECT", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(successfulUserQueryResponse())
                .thenReturn(successfulImageQueryResponse())
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        10003,
                        "invalid loan state",
                        objectMapper.readTree("{}")
                ));

        LoanApplyResponse response = loanApplicationService.apply("mem-test-001", "cid-test-001", buildApplyRequest());

        assertThat(response.applicationId()).isNull();
        assertThat(response.status()).isEqualTo("loan_failed");
        assertThat(response.benefitOrderNo()).isEqualTo("BEN-REJECT");
        assertThat(response.message()).isEqualTo("权益购买成功，借款申请失败：invalid loan state");
        assertThat(output)
                .contains("scene=loan apply")
                .contains("errorNo=YUNKA_UPSTREAM_REJECTED")
                .contains("path=/loan/apply");
        assertThat(countOccurrences(output.getOut(), "errorNo=YUNKA_UPSTREAM_REJECTED")).isEqualTo(1);
    }

    @Test
    void shouldEnqueueCompensationAndSavePendingReviewMappingWhenLoanApplyTimesOut(CapturedOutput output)
            throws Exception {
        when(loanApplicationGateway.findLatestPendingMapping("mem-test-001")).thenReturn(null);
        when(benefitOrderService.createLocalOrder(eq("mem-test-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-TIMEOUT", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(successfulUserQueryResponse())
                .thenReturn(successfulImageQueryResponse())
                .thenThrow(new UpstreamTimeoutException("Yunka gateway timeout"));

        LoanApplyResponse response = loanApplicationService.apply("mem-test-001", "cid-test-001", buildApplyRequest());

        assertThat(response.applicationId()).startsWith("APP-");
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.benefitOrderNo()).isEqualTo("BEN-TIMEOUT");

        ArgumentCaptor<LoanApplicationGateway.SaveCommand> saveCaptor =
                ArgumentCaptor.forClass(LoanApplicationGateway.SaveCommand.class);
        verify(loanApplicationGateway).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().memberId()).isEqualTo("mem-test-001");
        assertThat(saveCaptor.getValue().externalUserId()).isEqualTo("cid-test-001");
        assertThat(saveCaptor.getValue().applicationId()).isEqualTo(response.applicationId());
        assertThat(saveCaptor.getValue().benefitOrderNo()).isEqualTo("BEN-TIMEOUT");
        assertThat(saveCaptor.getValue().mappingStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(saveCaptor.getValue().platformLoanId()).isPositive();

        ArgumentCaptor<AsyncCompensationEnqueueService.EnqueueCommand> enqueueCaptor =
                ArgumentCaptor.forClass(AsyncCompensationEnqueueService.EnqueueCommand.class);
        verify(asyncCompensationEnqueueService).enqueue(enqueueCaptor.capture());
        assertThat(enqueueCaptor.getValue().taskType()).isEqualTo("YUNKA_LOAN_APPLY_RETRY");
        assertThat(enqueueCaptor.getValue().bizKey()).isEqualTo("LOAN_APPLY:" + response.applicationId());
        assertThat(enqueueCaptor.getValue().bizOrderNo()).isEqualTo(response.applicationId());
        assertThat(enqueueCaptor.getValue().requestPath()).isEqualTo("/api/gateway/proxy");
        assertThat(enqueueCaptor.getValue().requestPayload())
                .isInstanceOf(AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry.class);
        AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry payload =
                (AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry) enqueueCaptor.getValue().requestPayload();
        assertThat(payload.path()).isEqualTo("/loan/apply");
        assertThat(payload.memberId()).isEqualTo("mem-test-001");
        assertThat(payload.benefitOrderNo()).isEqualTo("BEN-TIMEOUT");
        assertThat(payload.uid()).isEqualTo("cid-test-001");
        assertThat(payload.applyId()).isEqualTo(response.applicationId());
        assertThat(payload.loanId()).isPositive();
        assertThat(output)
                .contains("scene=loan apply")
                .contains("errorNo=YUNKA_UPSTREAM_TIMEOUT");
        assertThat(countOccurrences(output.getOut(), "errorNo=YUNKA_UPSTREAM_TIMEOUT")).isEqualTo(1);
    }

    @Test
    void shouldNotSendLoanApplyWhenUserQueryRequiredFieldsAreMissing(CapturedOutput output) throws Exception {
        when(loanApplicationGateway.findLatestPendingMapping("mem-test-001")).thenReturn(null);
        when(benefitOrderService.createLocalOrder(eq("mem-test-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-USER-MISSING", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {
                          "idInfo": {"name": "张三", "idno": "310101199001011111", "phone": "13800138000"},
                          "contactInfo": [],
                          "supplementInfo": {"occupation": "20001"},
                          "optionInfo": {"maritalStatus": "50002"}
                        }
                        """)
        ));

        LoanApplyResponse response = loanApplicationService.apply("mem-test-001", "cid-test-001",
                buildApplyRequestWithSparseDocumentFields(null));

        assertThat(response.status()).isEqualTo("loan_failed");
        assertThat(response.message()).contains("LOAN_APPLY_USER_PROFILE_INCOMPLETE");
        assertThat(output)
                .contains("benefitOrderNo=BEN-USER-MISSING")
                .contains("errorNo=LOAN_APPLY_USER_PROFILE_INCOMPLETE")
                .contains("errorMsg=basicInfo is required for loan apply");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> yunkaCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(yunkaCaptor.capture());
        assertThat(yunkaCaptor.getValue().path()).isEqualTo("/user/query");
    }

    @Test
    void shouldNotSendLoanApplyWhenCreditImagesAreMissing(CapturedOutput output) throws Exception {
        when(loanApplicationGateway.findLatestPendingMapping("mem-test-001")).thenReturn(null);
        when(benefitOrderService.createLocalOrder(eq("mem-test-001"), any()))
                .thenReturn(new CreateBenefitOrderResponse("BEN-IMAGE-MISSING", "FIRST_DEDUCT_PENDING", "/redirect"));
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(successfulUserQueryResponse())
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        0,
                        "SUCCESS",
                        objectMapper.readTree("{}")
                ));

        LoanApplyResponse response = loanApplicationService.apply("mem-test-001", "cid-test-001",
                buildApplyRequestWithSparseDocumentFields(null));

        assertThat(response.status()).isEqualTo("loan_failed");
        assertThat(response.message()).contains("LOAN_APPLY_IMAGE_INFO_MISSING");
        assertThat(output)
                .contains("benefitOrderNo=BEN-IMAGE-MISSING")
                .contains("errorNo=LOAN_APPLY_IMAGE_INFO_MISSING")
                .contains("errorMsg=credit image query data is required for loan apply");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> yunkaCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, org.mockito.Mockito.times(2)).proxy(yunkaCaptor.capture());
        assertThat(yunkaCaptor.getAllValues().get(0).path()).isEqualTo("/user/query");
        assertThat(yunkaCaptor.getAllValues().get(1).path()).isEqualTo("/credit/image/query");
    }

    @Test
    void shouldReuseExistingPendingLoanChainInsteadOfCreatingAnotherOne() throws Exception {
        LoanApplicationMapping pendingMapping = new LoanApplicationMapping();
        pendingMapping.setApplicationId("APP-PENDING-001");
        pendingMapping.setBenefitOrderNo("BEN-PENDING-001");
        pendingMapping.setMappingStatus("PENDING_REVIEW");
        when(loanApplicationGateway.findLatestPendingMapping("mem-test-001")).thenReturn(pendingMapping);

        LoanApplyResponse firstRetry = loanApplicationService.apply("mem-test-001", "cid-test-001", buildApplyRequest());
        LoanApplyResponse secondRetry = loanApplicationService.apply("mem-test-001", "cid-test-001", buildApplyRequest());

        assertThat(firstRetry.applicationId()).isEqualTo("APP-PENDING-001");
        assertThat(firstRetry.benefitOrderNo()).isEqualTo("BEN-PENDING-001");
        assertThat(firstRetry.status()).isEqualTo("pending");
        assertThat(secondRetry.applicationId()).isEqualTo("APP-PENDING-001");
        assertThat(secondRetry.benefitOrderNo()).isEqualTo("BEN-PENDING-001");
        verifyNoInteractions(benefitOrderService, yunkaGatewayClient, asyncCompensationEnqueueService);
        verify(loanApplicationGateway, org.mockito.Mockito.times(2)).findLatestPendingMapping("mem-test-001");
        verifyNoMoreInteractions(loanApplicationGateway);
    }

    @Test
    void shouldRejectUnsupportedReceivingAccountBeforeCreatingBenefitOrder() throws Exception {
        when(memberReceivingAccountService.getReceivingAccount("mem-test-001", "acc_invalid"))
                .thenThrow(new BizException("MEMBER_RECEIVING_ACCOUNT_NOT_FOUND",
                        "Receiving account does not belong to current member"));
        LoanApplyRequest invalidRequest = new LoanApplyRequest(
                3000L,
                299L,
                3,
                "acc_invalid",
                List.of("loan", "user"),
                "rent",
                "DAILY_CONSUMPTION",
                "6222020202028648",
                objectMapper.readTree("{\"education\":\"BACHELOR\"}"),
                objectMapper.readTree("{\"cidExpireDate\":\"2036-01-01\"}"),
                objectMapper.readTree("[{\"name\":\"张三\",\"mobile\":\"13800000001\",\"relation\":\"SPOUSE\"}]"),
                objectMapper.readTree("{\"occupation\":\"ENGINEER\"}"),
                objectMapper.readTree("{\"channel\":\"ABS_H5\"}"),
                objectMapper.readTree("[{\"type\":\"FACE\",\"base64\":\"abc\"}]"),
                "PBEN-001"
        );

        assertThatThrownBy(() -> loanApplicationService.apply("mem-test-001", "cid-test-001", invalidRequest))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorMsg())
                .isEqualTo("Receiving account does not belong to current member");

        verifyNoInteractions(benefitOrderService, asyncCompensationEnqueueService, yunkaGatewayClient);
    }

    private LoanApplyRequest buildApplyRequest() throws Exception {
        return buildApplyRequest("PBEN-001");
    }

    private LoanApplyRequest buildApplyRequest(String platformBenefitOrderNo) throws Exception {
        return new LoanApplyRequest(
                3000L,
                299L,
                3,
                "acc_001",
                List.of("loan", "user"),
                "rent",
                "DAILY_CONSUMPTION",
                "6222020202028648",
                objectMapper.readTree("""
                        {"education":"BACHELOR","monthlyIncome":"10000-15000"}
                        """),
                objectMapper.readTree("""
                        {"cidExpireDate":"2036-01-01"}
                        """),
                objectMapper.readTree("""
                        [{"name":"张三","mobile":"13800000001","relation":"SPOUSE"}]
                        """),
                objectMapper.readTree("""
                        {"occupation":"ENGINEER"}
                        """),
                objectMapper.readTree("""
                        {"channel":"ABS_H5"}
                        """),
                objectMapper.readTree("""
                        [{"type":"FACE","base64":"abc"}]
                        """),
                platformBenefitOrderNo
        );
    }

    private LoanApplyRequest buildApplyRequestWithSparseDocumentFields(String platformBenefitOrderNo) throws Exception {
        return new LoanApplyRequest(
                3000L,
                299L,
                3,
                "acc_001",
                List.of("loan", "user"),
                "shopping",
                null,
                "6222020202028648",
                null,
                null,
                null,
                null,
                null,
                null,
                platformBenefitOrderNo
        );
    }

    private YunkaGatewayClient.YunkaGatewayResponse successfulUserQueryResponse() throws Exception {
        return new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {
                          "userId": "mem-test-001",
                          "phone": "13800138000",
                          "basicInfo": {
                            "monthlyIncome": 10000,
                            "addressProvince": "上海",
                            "addressCity": "上海市",
                            "addressDistrict": "浦东新区",
                            "addressDetail": "世纪大道1号"
                          },
                          "idInfo": {
                            "name": "张三",
                            "idno": "310101199001011111",
                            "gender": 1,
                            "address": "上海市浦东新区",
                            "issuedBy": "上海公安",
                            "validStartDate": "2017.08.21",
                            "validEndDate": "2037.08.21",
                            "nation": "汉"
                          },
                          "contactInfo": [
                            {"name": "李四", "phone": "13900139000", "relation": "80003", "sort": 1}
                          ],
                          "supplementInfo": {"occupation": "20001"},
                          "optionInfo": {"maritalStatus": "50002"}
                        }
                        """)
        );
    }

    private YunkaGatewayClient.YunkaGatewayResponse successfulImageQueryResponse() throws Exception {
        return new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {
                          "back": "BACK_BASE64_LONG_VALUE",
                          "front": "FRONT_BASE64_LONG_VALUE",
                          "nature": "NATURE_BASE64_LONG_VALUE"
                        }
                        """)
        );
    }

    private YunkaGatewayClient.YunkaGatewayResponse successfulLoanApplyResponse(Integer loanId) throws Exception {
        return new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                objectMapper.readTree("""
                        {
                          "loanId": %d,
                          "status": "4002",
                          "remark": "处理中"
                        }
                        """.formatted(loanId))
        );
    }

    private H5LoanProperties h5LoanProperties() {
        return new H5LoanProperties(
                new H5LoanProperties.AmountRange(100L, 5000L, 100L, 3000L),
                List.of(new H5LoanProperties.TermOption("3期", 3)),
                BigDecimal.valueOf(0.18),
                "XX商业银行"
        );
    }

    private H5BenefitsProperties h5BenefitsProperties() {
        return new H5BenefitsProperties(
                "HUXUAN_CARD",
                true,
                false,
                "https://benefits.test",
                new H5BenefitsProperties.Activate(300000L, 30000L, "huixuan_card", "惠选卡开通成功"),
                new H5BenefitsProperties.Detail(
                        "惠选卡",
                        300L,
                        448L,
                        List.of(new H5BenefitsProperties.Feature("f1", "d1")),
                        List.of(),
                        List.of("tip"),
                        List.of()
                )
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

    private int countOccurrences(String text, String fragment) {
        return text.split(java.util.regex.Pattern.quote(fragment), -1).length - 1;
    }
}
