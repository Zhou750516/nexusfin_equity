package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.JointLoginRequest;
import com.nexusfin.equity.dto.request.RepaymentLoginRequest;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.RepaymentLoginServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepaymentLoginServiceTest {

    @Mock
    private JointLoginService jointLoginService;

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    @Test
    void shouldReuseJointLoginAndValidateLoanOwnership() {
        RepaymentLoginService service = new RepaymentLoginServiceImpl(
                jointLoginService,
                memberInfoRepository,
                loanApplicationMappingRepository
        );
        when(jointLoginService.login(any())).thenReturn(new JointLoginService.JointLoginResult(
                "jwt-repayment-token",
                "push",
                "landing",
                null,
                "xh-cid-001",
                true
        ));
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId("mem-repay-login");
        memberInfo.setExternalUserId("xh-cid-001");
        when(memberInfoRepository.selectByTechPlatformUserId("xh-cid-001")).thenReturn(null);
        when(memberInfoRepository.selectByCid("xh-cid-001")).thenReturn(memberInfo);
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setMemberId("mem-repay-login");
        mapping.setPlatformLoanId(1781594032);
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(mapping);

        RepaymentLoginService.RepaymentLoginResult result = service.login(
                new RepaymentLoginRequest("joint-token-001", 1781594032)
        );

        ArgumentCaptor<JointLoginRequest> jointRequestCaptor = ArgumentCaptor.forClass(JointLoginRequest.class);
        verify(jointLoginService).login(jointRequestCaptor.capture());
        assertThat(jointRequestCaptor.getValue().token()).isEqualTo("joint-token-001");
        assertThat(jointRequestCaptor.getValue().scene()).isEqualTo("push");
        verify(loanApplicationMappingRepository).selectOne(any());
        assertThat(result.jwtToken()).isEqualTo("jwt-repayment-token");
        assertThat(result.loanId()).isEqualTo(1781594032);
    }

    @Test
    void shouldRejectLoanIdNotOwnedByJointLoginMember() {
        RepaymentLoginService service = new RepaymentLoginServiceImpl(
                jointLoginService,
                memberInfoRepository,
                loanApplicationMappingRepository
        );
        when(jointLoginService.login(any())).thenReturn(new JointLoginService.JointLoginResult(
                "jwt-repayment-token",
                "push",
                "landing",
                null,
                "xh-cid-001",
                true
        ));
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId("mem-repay-login");
        memberInfo.setExternalUserId("xh-cid-001");
        when(memberInfoRepository.selectByTechPlatformUserId("xh-cid-001")).thenReturn(memberInfo);
        when(loanApplicationMappingRepository.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.login(new RepaymentLoginRequest("joint-token-001", 1781594032)))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("REPAYMENT_LOGIN_LOAN_FORBIDDEN");
    }

    @Test
    void shouldTranslateJointTokenInvalidToRepaymentLoginTokenInvalid() {
        RepaymentLoginService service = new RepaymentLoginServiceImpl(
                jointLoginService,
                memberInfoRepository,
                loanApplicationMappingRepository
        );
        when(jointLoginService.login(any()))
                .thenThrow(new BizException("JOINT_LOGIN_TOKEN_INVALID", "Joint login session expired"));

        assertThatThrownBy(() -> service.login(new RepaymentLoginRequest("expired-token", 1781594032)))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("REPAYMENT_LOGIN_TOKEN_INVALID");

        verify(memberInfoRepository, never()).selectByTechPlatformUserId(any());
        verify(loanApplicationMappingRepository, never()).selectOne(any());
    }
}
