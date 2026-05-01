package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.BenefitRedirectUrlRequest;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.impl.BenefitRedirectUrlServiceImpl;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlRequest;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlResponse;
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
class BenefitRedirectUrlServiceTest {

    @Mock
    private JointLoginService jointLoginService;

    @Mock
    private QwBenefitClient qwBenefitClient;

    @Test
    void shouldReturnCurrentQwExerciseRedirectUrlForBenefitSyncContract() {
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient);
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
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient);
        when(jointLoginService.login(any()))
                .thenThrow(new BizException("JOINT_LOGIN_TOKEN_INVALID", "Joint login session expired"));

        assertThatThrownBy(() -> service.generate(new BenefitRedirectUrlRequest("joint-token-redirect-002", "BEN-REDIRECT-002")))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("JOINT_LOGIN_TOKEN_INVALID");
    }

    @Test
    void shouldTranslateQwFailureToControlledBusinessError() {
        BenefitRedirectUrlServiceImpl service = new BenefitRedirectUrlServiceImpl(jointLoginService, qwBenefitClient);
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
    }
}
