package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.MemberReceivingAccount;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberReceivingAccountRepository;
import com.nexusfin.equity.service.impl.MemberReceivingAccountServiceImpl;
import java.util.List;
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
    void shouldCacheAllUserCardsWithSourceIndex() {
        MemberReceivingAccount existing = new MemberReceivingAccount();
        existing.setId(11L);
        existing.setMemberId("mem-test-001");
        existing.setAccountId("card-second-002");
        existing.setBankName("旧银行");
        existing.setLastFour("0000");
        existing.setAccountStatus("ACTIVE");
        existing.setIsDefault(0);
        existing.setSource("YUNKA_USER_CARDS");
        existing.setSourceIndex(9);
        when(memberReceivingAccountRepository.selectByMemberId("mem-test-001"))
                .thenReturn(List.of(existing));

        MemberReceivingAccountService service = new MemberReceivingAccountServiceImpl(memberReceivingAccountRepository);

        service.cacheReceivingAccounts("mem-test-001", List.of(
                new MemberReceivingAccountService.CardCacheCommand(
                        "card-first-001", "第一银行", "1111", 0, 0, "YUNKA_USER_CARDS"),
                new MemberReceivingAccountService.CardCacheCommand(
                        "card-second-002", "第二银行", "2222", 1, 1, "YUNKA_USER_CARDS")
        ));

        ArgumentCaptor<MemberReceivingAccount> insertCaptor = ArgumentCaptor.forClass(MemberReceivingAccount.class);
        verify(memberReceivingAccountRepository).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getAccountId()).isEqualTo("card-first-001");
        assertThat(insertCaptor.getValue().getSourceIndex()).isZero();
        assertThat(insertCaptor.getValue().getSource()).isEqualTo("YUNKA_USER_CARDS");

        ArgumentCaptor<MemberReceivingAccount> updateCaptor = ArgumentCaptor.forClass(MemberReceivingAccount.class);
        verify(memberReceivingAccountRepository).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getAccountId()).isEqualTo("card-second-002");
        assertThat(updateCaptor.getValue().getBankName()).isEqualTo("第二银行");
        assertThat(updateCaptor.getValue().getSourceIndex()).isEqualTo(1);
    }

    @Test
    void shouldReactivateExistingCachedCardInsteadOfInsertingDuplicate() {
        MemberReceivingAccount inactive = new MemberReceivingAccount();
        inactive.setId(22L);
        inactive.setMemberId("mem-test-001");
        inactive.setAccountId("card-returned-001");
        inactive.setBankName("旧银行");
        inactive.setLastFour("0000");
        inactive.setAccountStatus("INACTIVE");
        inactive.setIsDefault(0);
        inactive.setSource("YUNKA_USER_CARDS");
        inactive.setSourceIndex(5);
        when(memberReceivingAccountRepository.selectByMemberId("mem-test-001"))
                .thenReturn(List.of(inactive));

        MemberReceivingAccountService service = new MemberReceivingAccountServiceImpl(memberReceivingAccountRepository);

        service.cacheReceivingAccounts("mem-test-001", List.of(
                new MemberReceivingAccountService.CardCacheCommand(
                        "card-returned-001", "回归银行", "6789", 0, 0, "YUNKA_USER_CARDS")
        ));

        verify(memberReceivingAccountRepository, never()).insert(any());
        ArgumentCaptor<MemberReceivingAccount> updateCaptor = ArgumentCaptor.forClass(MemberReceivingAccount.class);
        verify(memberReceivingAccountRepository).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(22L);
        assertThat(updateCaptor.getValue().getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(updateCaptor.getValue().getBankName()).isEqualTo("回归银行");
        assertThat(updateCaptor.getValue().getSourceIndex()).isZero();
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
