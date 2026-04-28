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
                  "uid": "tech-user-001",
                  "benefitOrderNo": "ord-001",
                  "applyId": "APP-001",
                  "loanId": "LN-001",
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_001"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-001");
        mapping.setMappingStatus("PENDING_REVIEW");
        mapping.setExternalUserId("tech-user-001");
        mapping.setUpstreamQueryValue("LN-001");
        when(loanApplicationMappingRepository.selectById("APP-001")).thenReturn(mapping);
        when(yunkaGatewayClient.proxy(any())).thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                0,
                "SUCCESS",
                JsonNodeFactory.instance.objectNode().put("loanId", "LN-001")
        ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        assertThat(requestCaptor.getValue().requestId()).isEqualTo("LA-001");
        assertThat(requestCaptor.getValue().path()).isEqualTo("/loan/query");
        assertThat(requestCaptor.getValue().bizOrderNo()).isEqualTo("APP-001");
        assertThat(result.responsePayload()).contains("SUCCESS");
        assertThat(result.responsePayload()).contains("LN-001");
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
                  "uid": "tech-user-003",
                  "benefitOrderNo": "ord-003",
                  "applyId": "APP-003",
                  "loanId": "LN-003",
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_003"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-003");
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
                  "uid": "tech-user-004",
                  "benefitOrderNo": "ord-004",
                  "applyId": "APP-004",
                  "loanId": "LN-004",
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_004"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-004");
        mapping.setExternalUserId("tech-user-004");
        mapping.setUpstreamQueryValue("LN-004");
        mapping.setMappingStatus("PENDING_REVIEW");
        when(loanApplicationMappingRepository.selectById("APP-004")).thenReturn(mapping);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        0,
                        "SUCCESS",
                        JsonNodeFactory.instance.objectNode().put("status", "7002").put("loanId", "LN-004")
                ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        assertThat(result.responsePayload()).contains("7002");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        assertThat(requestCaptor.getValue().path()).isEqualTo("/loan/query");
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
                  "uid": "tech-user-002",
                  "benefitOrderNo": "ord-002",
                  "applyId": "APP-002",
                  "loanId": "LN-002",
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
                  "uid": "tech-user-005",
                  "benefitOrderNo": "ord-005",
                  "applyId": "APP-005",
                  "loanId": "LN-005",
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_005"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-005");
        mapping.setExternalUserId("tech-user-005");
        mapping.setUpstreamQueryValue("LN-005");
        mapping.setMappingStatus("PENDING_REVIEW");
        when(loanApplicationMappingRepository.selectById("APP-005")).thenReturn(mapping);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", null))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        0,
                        "SUCCESS",
                        JsonNodeFactory.instance.objectNode().put("loanId", "LN-005")
                ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        assertThat(result.responsePayload()).contains("LN-005");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, org.mockito.Mockito.times(2)).proxy(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).path()).isEqualTo("/loan/query");
        assertThat(requestCaptor.getAllValues().get(1).path()).isEqualTo("/loan/apply");
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
                  "uid": "tech-user-006",
                  "benefitOrderNo": "ord-006",
                  "applyId": "APP-006",
                  "loanId": "LN-006",
                  "loanAmount": 300000,
                  "loanPeriod": 3,
                  "bankCardNo": "acc_006"
                }
                """);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId("APP-006");
        mapping.setExternalUserId("tech-user-006");
        mapping.setUpstreamQueryValue("LN-006");
        mapping.setMappingStatus("PENDING_REVIEW");
        when(loanApplicationMappingRepository.selectById("APP-006")).thenReturn(mapping);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(null)
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        0,
                        "SUCCESS",
                        JsonNodeFactory.instance.objectNode().put("loanId", "LN-006")
                ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        assertThat(result.responsePayload()).contains("LN-006");
        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, org.mockito.Mockito.times(2)).proxy(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).path()).isEqualTo("/loan/query");
        assertThat(requestCaptor.getAllValues().get(1).path()).isEqualTo("/loan/apply");
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
                  "uid": "tech-user-007",
                  "benefitOrderNo": "ord-007",
                  "applyId": "APP-007",
                  "loanId": "LN-007",
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
                        "/loan/trail",
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
                        "/benefit/sync"
                )
        );
    }
}
