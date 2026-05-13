package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.MemberReceivingAccount;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberReceivingAccountRepository;
import com.nexusfin.equity.service.impl.MemberReceivingAccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberReceivingAccountServiceTest {

    @Mock
    private MemberReceivingAccountRepository memberReceivingAccountRepository;

    @Test
    void shouldReturnDefaultReceivingAccountForMember() {
        MemberReceivingAccount account = new MemberReceivingAccount();
        account.setMemberId("mem-test-001");
        account.setAccountId("acc-db-001");
        account.setBankName("测试银行");
        account.setLastFour("1234");
        when(memberReceivingAccountRepository.selectDefaultActive("mem-test-001")).thenReturn(account);

        MemberReceivingAccountService service = new MemberReceivingAccountServiceImpl(memberReceivingAccountRepository);

        MemberReceivingAccountService.ReceivingAccountDetails result =
                service.getDefaultReceivingAccount("mem-test-001");

        assertThat(result.accountId()).isEqualTo("acc-db-001");
        assertThat(result.bankName()).isEqualTo("测试银行");
        assertThat(result.lastFour()).isEqualTo("1234");
    }

    @Test
    void shouldThrowControlledExceptionWhenMemberHasNoDefaultReceivingAccount() {
        when(memberReceivingAccountRepository.selectDefaultActive("mem-test-001")).thenReturn(null);

        MemberReceivingAccountService service = new MemberReceivingAccountServiceImpl(memberReceivingAccountRepository);

        assertThatThrownBy(() -> service.getDefaultReceivingAccount("mem-test-001"))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorNo())
                .isEqualTo("MEMBER_RECEIVING_ACCOUNT_NOT_CONFIGURED");
    }

    @Test
    void shouldUpsertDefaultReceivingAccountForMember() {
        when(memberReceivingAccountRepository.selectByMemberIdAndAccountId("mem-test-001", "acc-db-001"))
                .thenReturn(null);

        MemberReceivingAccountService service = new MemberReceivingAccountServiceImpl(memberReceivingAccountRepository);

        service.upsertDefaultReceivingAccount(
                "mem-test-001",
                new MemberReceivingAccountService.UpsertCommand("acc-db-001", "测试银行", "1234", "JOINT_LOGIN")
        );

        ArgumentCaptor<MemberReceivingAccount> captor = ArgumentCaptor.forClass(MemberReceivingAccount.class);
        verify(memberReceivingAccountRepository).insert(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo("mem-test-001");
        assertThat(captor.getValue().getAccountId()).isEqualTo("acc-db-001");
        assertThat(captor.getValue().getBankName()).isEqualTo("测试银行");
        assertThat(captor.getValue().getLastFour()).isEqualTo("1234");
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
        assertThat(captor.getValue().getSource()).isEqualTo("JOINT_LOGIN");
    }

    @Test
    void shouldRejectReceivingAccountThatDoesNotBelongToMember() {
        when(memberReceivingAccountRepository.selectByMemberIdAndAccountId("mem-test-001", "acc-other"))
                .thenReturn(null);

        MemberReceivingAccountService service = new MemberReceivingAccountServiceImpl(memberReceivingAccountRepository);

        assertThatThrownBy(() -> service.getReceivingAccount("mem-test-001", "acc-other"))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorNo())
                .isEqualTo("MEMBER_RECEIVING_ACCOUNT_NOT_FOUND");

        verify(memberReceivingAccountRepository, never()).insert(any());
    }
}
