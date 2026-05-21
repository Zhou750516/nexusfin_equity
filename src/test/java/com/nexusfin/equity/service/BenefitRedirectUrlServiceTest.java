package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.BenefitRedirectUrlRequest;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.impl.BenefitRedirectUrlServiceImpl;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlRequest;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlResponse;
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
class BenefitRedirectUrlServiceTest {

    @Mock
    private JointLoginService jointLoginService;

    @Mock
    private QwBenefitClient qwBenefitClient;

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Test
    void shouldReturnCurrentQwExerciseRedirectUrlForBenefitSyncContract() {
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient, benefitOrderRepository);
        BenefitRedirectUrlRequest request = new BenefitRedirectUrlRequest("joint-token-redirect-001", "BEN-REDIRECT-001");
        when(jointLoginService.login(any())).thenReturn(new JointLoginService.JointLoginResult(
                "jwt-joint-token",
                "exercise",
                "joint-dispatch",
                "BEN-REDIRECT-001",
                "xh-cid-redirect-001",
                true
        ));
        when(qwBenefitClient.getExerciseUrl(any())).thenReturn(new QwExerciseUrlResponse(
                0,
                "https://redirect.test/benefit",
                "qw-token-001",
                "2026-04-30 10:00:00",
                "2027-04-30 10:00:00"
        ));

        BenefitRedirectUrlService.BenefitRedirectUrlResult result = service.generate(request);

        assertThat(result.redirectUrl()).isEqualTo("https://redirect.test/benefit");
        ArgumentCaptor<QwExerciseUrlRequest> requestCaptor = ArgumentCaptor.forClass(QwExerciseUrlRequest.class);
        verify(qwBenefitClient).getExerciseUrl(requestCaptor.capture());
        assertThat(requestCaptor.getValue().uniqueId()).isEqualTo("xh-cid-redirect-001");
        assertThat(requestCaptor.getValue().partnerOrderNo()).isEqualTo("BEN-REDIRECT-001");
    }

    @Test
    void shouldPropagateControlledErrorWhenJointLoginFails() {
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient, benefitOrderRepository);
        when(jointLoginService.login(any()))
                .thenThrow(new BizException("JOINT_LOGIN_TOKEN_INVALID", "Joint login session expired"));

        assertThatThrownBy(() -> service.generate(new BenefitRedirectUrlRequest("joint-token-redirect-002", "BEN-REDIRECT-002")))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("JOINT_LOGIN_TOKEN_INVALID");
    }

    @Test
    void shouldTranslateQwFailureToControlledBusinessError(CapturedOutput output) {
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient, benefitOrderRepository);
        when(jointLoginService.login(any())).thenReturn(new JointLoginService.JointLoginResult(
                "jwt-joint-token",
                "exercise",
                "joint-dispatch",
                "BEN-REDIRECT-003",
                "xh-cid-redirect-003",
                true
        ));
        when(qwBenefitClient.getExerciseUrl(any()))
                .thenThrow(new BizException("QW_UPSTREAM_REJECTED", "member card inactive"));

        assertThatThrownBy(() -> service.generate(new BenefitRedirectUrlRequest("joint-token-redirect-003", "BEN-REDIRECT-003")))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("REDRECT_BENEFIT_URL_UPSTREAM_FAILED");
        assertThat(output).contains("benefit redirect qw exercise redirect url failed");
        assertThat(output).contains("errorNo=QW_UPSTREAM_REJECTED");
        assertThat(output).contains("errorMsg=member card inactive");
    }

    @Test
    void shouldLogErrorFieldsWhenQwExerciseRedirectTimesOut(CapturedOutput output) {
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient, benefitOrderRepository);
        when(jointLoginService.login(any())).thenReturn(new JointLoginService.JointLoginResult(
                "jwt-joint-token",
                "exercise",
                "joint-dispatch",
                "BEN-REDIRECT-004",
                "xh-cid-redirect-004",
                true
        ));
        when(qwBenefitClient.getExerciseUrl(any()))
                .thenThrow(new UpstreamTimeoutException("QW gateway timeout"));

        assertThatThrownBy(() -> service.generate(new BenefitRedirectUrlRequest("joint-token-redirect-004", "BEN-REDIRECT-004")))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("REDRECT_BENEFIT_URL_UPSTREAM_TIMEOUT");
        assertThat(output).contains("benefit redirect qw exercise redirect url timed out");
        assertThat(output).contains("errorNo=REDRECT_BENEFIT_URL_UPSTREAM_TIMEOUT");
        assertThat(output).contains("errorMsg=QW gateway timeout");
    }

    @Test
    void shouldGenerateRedirectUrlForAuthenticatedMemberOwnedQwOrderNo() {
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient, benefitOrderRepository);
        when(benefitOrderRepository.selectOne(any())).thenReturn(order("ord-001", "mem-redirect-001", "QW-ORDER-001", "user-qw-001"));
        when(qwBenefitClient.getExerciseUrl(any())).thenReturn(new QwExerciseUrlResponse(
                0,
                "https://redirect.test/member-benefit",
                "qw-token-005",
                "2026-04-30 10:00:00",
                "2027-04-30 10:00:00"
        ));

        BenefitRedirectUrlService.BenefitRedirectUrlResult result =
                service.generateForMember("mem-redirect-001", "QW-ORDER-001");

        assertThat(result.redirectUrl()).isEqualTo("https://redirect.test/member-benefit");
        ArgumentCaptor<QwExerciseUrlRequest> requestCaptor = ArgumentCaptor.forClass(QwExerciseUrlRequest.class);
        verify(qwBenefitClient).getExerciseUrl(requestCaptor.capture());
        assertThat(requestCaptor.getValue().uniqueId()).isEqualTo("user-qw-001");
        assertThat(requestCaptor.getValue().partnerOrderNo()).isEqualTo("ord-001");
    }

    @Test
    void shouldRejectMemberRedirectWhenQwOrderNoDoesNotBelongToMember() {
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient, benefitOrderRepository);
        when(benefitOrderRepository.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.generateForMember("mem-redirect-other", "QW-ORDER-001"))
                .isInstanceOf(BizException.class)
                .satisfies(throwable -> {
                    BizException exception = (BizException) throwable;
                    assertThat(exception.getCode()).isEqualTo(403);
                    assertThat(exception.getErrorNo()).isEqualTo("BENEFIT_REDIRECT_FORBIDDEN");
                });
    }

    @Test
    void shouldRejectMemberRedirectWhenExternalUserIdIsMissing() {
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient, benefitOrderRepository);
        when(benefitOrderRepository.selectOne(any())).thenReturn(order("ord-002", "mem-redirect-002", "QW-ORDER-002", ""));

        assertThatThrownBy(() -> service.generateForMember("mem-redirect-002", "QW-ORDER-002"))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("BENEFIT_REDIRECT_MEMBER_NOT_BOUND");
    }

    private BenefitOrder order(String benefitOrderNo, String memberId, String qwOrderNo, String externalUserId) {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo(benefitOrderNo);
        order.setMemberId(memberId);
        order.setQwOrderNo(qwOrderNo);
        order.setExternalUserId(externalUserId);
        return order;
    }
}
