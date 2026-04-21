package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.service.impl.YunkaLoanApplyCompensationExecutor;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
                        yunkaGatewayClient,
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
                        yunkaGatewayClient,
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
                        yunkaGatewayClient,
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
                        yunkaGatewayClient,
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
                .hasMessageContaining("YUNKA_COMPENSATION_FAILED");
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
                        "/repay/query"
                )
        );
    }
}
