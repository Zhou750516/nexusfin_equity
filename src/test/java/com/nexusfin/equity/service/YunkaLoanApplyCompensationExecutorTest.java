package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.service.impl.YunkaLoanApplyCompensationExecutor;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.fasterxml.jackson.databind.JsonNode;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class YunkaLoanApplyCompensationExecutorTest {

    @Mock
    private YunkaGatewayClient yunkaGatewayClient;

    @Mock
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    @Test
    void shouldForwardTaskPayloadToYunkaGateway() {
        YunkaProperties yunkaProperties = buildYunkaProperties();
        YunkaLoanApplyCompensationExecutor executor =
                new YunkaLoanApplyCompensationExecutor(
                        new YunkaCallTemplate(yunkaGatewayClient),
                        loanApplicationMappingRepository,
                        yunkaProperties,
                        new ObjectMapper()
                );
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-yunka-1");
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setRequestPayload("""
                {
                  "requestId": "LA-001",
                  "path": "/loan/apply",
                  "bizOrderNo": "APP-001",
                  "memberId": "mem-user-001",
                  "uid": "tech-user-001",
                  "benefitOrderNo": "ord-001",
                  "applyId": "APP-001",
                  "loanId": 20260501,
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_001"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-001");
        mapping.setMemberId("mem-user-001");
        mapping.setMappingStatus("PENDING_REVIEW");
        mapping.setExternalUserId("tech-user-001");
        mapping.setPlatformLoanId(20260501);
        when(loanApplicationMappingRepository.selectById("APP-001")).thenReturn(mapping);
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                JsonNodeFactory.instance.objectNode().put("loanId", 20260501)
        ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        assertThat(requestCaptor.getValue().requestId()).isEqualTo("LA-001");
        assertThat(requestCaptor.getValue().path()).isEqualTo("/loan/query");
        assertThat(new ObjectMapper().valueToTree(requestCaptor.getValue()).has("bizOrderNo")).isFalse();
        JsonNode requestData = new ObjectMapper().valueToTree(requestCaptor.getValue().data());
        assertThat(requestData.path("userId").asText()).isEqualTo("mem-user-001");
        assertThat(requestData.path("loanId").isInt()).isTrue();
        assertThat(requestData.path("loanId").asInt()).isEqualTo(20260501);
        assertThat(requestData.path("userId").asText()).isNotEqualTo("tech-user-001");
        assertThat(requestData.has("uid")).isFalse();
        assertThat(result.responsePayload()).contains("SUCCESS");
        assertThat(result.responsePayload()).contains("20260501");
        ArgumentCaptor<LoanApplicationMapping> mappingCaptor = ArgumentCaptor.forClass(LoanApplicationMapping.class);
        verify(loanApplicationMappingRepository).updateById(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getApplicationId()).isEqualTo("APP-001");
        assertThat(mappingCaptor.getValue().getMappingStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldSkipApplyWhenLocalMappingAlreadyActive() {
        YunkaProperties yunkaProperties = buildYunkaProperties();
        YunkaLoanApplyCompensationExecutor executor =
                new YunkaLoanApplyCompensationExecutor(
                        new YunkaCallTemplate(yunkaGatewayClient),
                        loanApplicationMappingRepository,
                        yunkaProperties,
                        new ObjectMapper()
                );
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-yunka-active");
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setRequestPayload("""
                {
                  "requestId": "LA-003",
                  "path": "/loan/apply",
                  "bizOrderNo": "APP-003",
                  "memberId": "mem-user-003",
                  "uid": "tech-user-003",
                  "benefitOrderNo": "ord-003",
                  "applyId": "APP-003",
                  "loanId": 20260503,
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_003"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-003");
        mapping.setMemberId("mem-user-003");
        mapping.setMappingStatus("ACTIVE");
        when(loanApplicationMappingRepository.selectById("APP-003")).thenReturn(mapping);

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        assertThat(result.responsePayload()).contains("SKIPPED_ALREADY_ACTIVE");
        org.mockito.Mockito.verifyNoInteractions(yunkaGatewayClient);
    }

    @Test
    void shouldSkipApplyWhenPendingReviewAlreadyQueryableUpstream() {
        YunkaProperties yunkaProperties = buildYunkaProperties();
        YunkaLoanApplyCompensationExecutor executor =
                new YunkaLoanApplyCompensationExecutor(
                        new YunkaCallTemplate(yunkaGatewayClient),
                        loanApplicationMappingRepository,
                        yunkaProperties,
                        new ObjectMapper()
                );
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-yunka-query");
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setRequestPayload("""
                {
                  "requestId": "LA-004",
                  "path": "/loan/apply",
                  "bizOrderNo": "APP-004",
                  "memberId": "mem-user-004",
                  "uid": "tech-user-004",
                  "benefitOrderNo": "ord-004",
                  "applyId": "APP-004",
                  "loanId": 20260504,
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_004"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-004");
        mapping.setMemberId("mem-user-004");
        mapping.setExternalUserId("tech-user-004");
        mapping.setPlatformLoanId(20260504);
        mapping.setMappingStatus("PENDING_REVIEW");
        when(loanApplicationMappingRepository.selectById("APP-004")).thenReturn(mapping);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        0,
                        "SUCCESS",
                        JsonNodeFactory.instance.objectNode().put("status", "7002").put("loanId", 20260504)
                ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        assertThat(result.responsePayload()).contains("7002");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        assertThat(requestCaptor.getValue().path()).isEqualTo("/loan/query");
        JsonNode queryData = new ObjectMapper().valueToTree(requestCaptor.getValue().data());
        assertThat(queryData.path("userId").asText()).isEqualTo("mem-user-004");
        assertThat(queryData.path("loanId").isInt()).isTrue();
        assertThat(queryData.path("loanId").asInt()).isEqualTo(20260504);
        assertThat(queryData.path("userId").asText()).isNotEqualTo("tech-user-004");
        assertThat(queryData.has("uid")).isFalse();
    }

    @Test
    void shouldFailWhenYunkaReturnsNonZeroCode() {
        YunkaProperties yunkaProperties = buildYunkaProperties();
        YunkaLoanApplyCompensationExecutor executor =
                new YunkaLoanApplyCompensationExecutor(
                        new YunkaCallTemplate(yunkaGatewayClient),
                        loanApplicationMappingRepository,
                        yunkaProperties,
                        new ObjectMapper()
                );
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-yunka-fail");
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setRequestPayload("""
                {
                  "requestId": "LA-002",
                  "path": "/loan/apply",
                  "bizOrderNo": "APP-002",
                  "memberId": "mem-user-002",
                  "uid": "tech-user-002",
                  "benefitOrderNo": "ord-002",
                  "applyId": "APP-002",
                  "loanId": 20260502,
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_002"
                }
                """);
        when(loanApplicationMappingRepository.selectById("APP-002")).thenReturn(null);
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                502,
                "gateway unavailable",
                null
        ));

        assertThatThrownBy(() -> executor.execute(task))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo(ErrorCodes.YUNKA_UPSTREAM_REJECTED);
    }

    @Test
    void shouldCallLoanApplyWhenPendingReviewQueryHasNoUsableData() {
        YunkaProperties yunkaProperties = buildYunkaProperties();
        YunkaLoanApplyCompensationExecutor executor =
                new YunkaLoanApplyCompensationExecutor(
                        new YunkaCallTemplate(yunkaGatewayClient),
                        loanApplicationMappingRepository,
                        yunkaProperties,
                        new ObjectMapper()
                );
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-yunka-fallback");
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setRequestPayload("""
                {
                  "requestId": "LA-005",
                  "path": "/loan/apply",
                  "bizOrderNo": "APP-005",
                  "memberId": "mem-user-005",
                  "uid": "tech-user-005",
                  "benefitOrderNo": "ord-005",
                  "applyId": "APP-005",
                  "loanId": 20260505,
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_005"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-005");
        mapping.setMemberId("mem-user-005");
        mapping.setExternalUserId("tech-user-005");
        mapping.setPlatformLoanId(20260505);
        mapping.setMappingStatus("PENDING_REVIEW");
        when(loanApplicationMappingRepository.selectById("APP-005")).thenReturn(mapping);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", null))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        0,
                        "SUCCESS",
                        JsonNodeFactory.instance.objectNode().put("loanId", 20260505)
                ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        assertThat(result.responsePayload()).contains("20260505");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, org.mockito.Mockito.times(2)).proxy(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).path()).isEqualTo("/loan/query");
        assertThat(requestCaptor.getAllValues().get(1).path()).isEqualTo("/loan/apply");
        JsonNode queryPayload = new ObjectMapper().valueToTree(requestCaptor.getAllValues().get(0).data());
        JsonNode applyPayload = new ObjectMapper().valueToTree(requestCaptor.getAllValues().get(1).data());
        assertThat(queryPayload.path("userId").asText()).isEqualTo("mem-user-005");
        assertThat(queryPayload.path("loanId").isInt()).isTrue();
        assertThat(queryPayload.path("loanId").asInt()).isEqualTo(20260505);
        assertThat(queryPayload.path("userId").asText()).isNotEqualTo("tech-user-005");
        assertThat(queryPayload.has("uid")).isFalse();
        assertThat(applyPayload.path("userId").asText()).isEqualTo("mem-user-005");
        assertThat(applyPayload.path("loanId").isInt()).isTrue();
        assertThat(applyPayload.path("loanId").asInt()).isEqualTo(20260505);
        assertThat(applyPayload.path("userId").asText()).isNotEqualTo("tech-user-005");
        assertThat(applyPayload.has("uid")).isFalse();
        assertThat(applyPayload.path("loanAmount").decimalValue()).isEqualByComparingTo("3000.00");
    }

    @Test
    void shouldFallbackToLoanApplyWhenPendingReviewQueryReturnsNull() {
        YunkaProperties yunkaProperties = buildYunkaProperties();
        YunkaLoanApplyCompensationExecutor executor =
                new YunkaLoanApplyCompensationExecutor(
                        new YunkaCallTemplate(yunkaGatewayClient),
                        loanApplicationMappingRepository,
                        yunkaProperties,
                        new ObjectMapper()
                );
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-yunka-null-fallback");
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setRequestPayload("""
                {
                  "requestId": "LA-006",
                  "path": "/loan/apply",
                  "bizOrderNo": "APP-006",
                  "memberId": "mem-user-006",
                  "uid": "tech-user-006",
                  "benefitOrderNo": "ord-006",
                  "applyId": "APP-006",
                  "loanId": 20260506,
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_006"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-006");
        mapping.setMemberId("mem-user-006");
        mapping.setExternalUserId("tech-user-006");
        mapping.setPlatformLoanId(20260506);
        mapping.setMappingStatus("PENDING_REVIEW");
        when(loanApplicationMappingRepository.selectById("APP-006")).thenReturn(mapping);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(null)
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        0,
                        "SUCCESS",
                        JsonNodeFactory.instance.objectNode().put("loanId", 20260506)
                ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        assertThat(result.responsePayload()).contains("20260506");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, org.mockito.Mockito.times(2)).proxy(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).path()).isEqualTo("/loan/query");
        assertThat(requestCaptor.getAllValues().get(1).path()).isEqualTo("/loan/apply");
        JsonNode queryPayload = new ObjectMapper().valueToTree(requestCaptor.getAllValues().get(0).data());
        JsonNode applyPayload = new ObjectMapper().valueToTree(requestCaptor.getAllValues().get(1).data());
        assertThat(queryPayload.path("userId").asText()).isEqualTo("mem-user-006");
        assertThat(queryPayload.path("loanId").isInt()).isTrue();
        assertThat(queryPayload.path("loanId").asInt()).isEqualTo(20260506);
        assertThat(queryPayload.path("userId").asText()).isNotEqualTo("tech-user-006");
        assertThat(queryPayload.has("uid")).isFalse();
        assertThat(applyPayload.path("userId").asText()).isEqualTo("mem-user-006");
        assertThat(applyPayload.path("loanId").isInt()).isTrue();
        assertThat(applyPayload.path("loanId").asInt()).isEqualTo(20260506);
        assertThat(applyPayload.path("userId").asText()).isNotEqualTo("tech-user-006");
        assertThat(applyPayload.has("uid")).isFalse();
    }

    @Test
    void shouldNotLogSuccessWhenApplyRetryIsRejected(CapturedOutput output) {
        YunkaProperties yunkaProperties = buildYunkaProperties();
        YunkaLoanApplyCompensationExecutor executor =
                new YunkaLoanApplyCompensationExecutor(
                        new YunkaCallTemplate(yunkaGatewayClient),
                        loanApplicationMappingRepository,
                        yunkaProperties,
                        new ObjectMapper()
                );
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-yunka-reject-log");
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setRequestPayload("""
                {
                  "requestId": "LA-007",
                  "path": "/loan/apply",
                  "bizOrderNo": "APP-007",
                  "memberId": "mem-user-007",
                  "uid": "tech-user-007",
                  "benefitOrderNo": "ord-007",
                  "applyId": "APP-007",
                  "loanId": 20260507,
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_007"
                }
                """);
        when(loanApplicationMappingRepository.selectById("APP-007")).thenReturn(null);
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                502,
                "gateway unavailable",
                null
        ));

        assertThatThrownBy(() -> executor.execute(task))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo(ErrorCodes.YUNKA_UPSTREAM_REJECTED);

        assertThat(output)
                .contains("scene=yunka loan apply retry apply")
                .contains("errorNo=YUNKA_UPSTREAM_REJECTED")
                .doesNotContain("scene=yunka loan apply retry apply elapsedMs=0 yunka request success");
    }

    private YunkaProperties buildYunkaProperties() {
        return new YunkaProperties(
                true,
                "REST",
                "https://yunka.test",
                "/api/gateway/proxy",
                2000,
                3000,
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
