package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.BankCardSignApplyRequest;
import com.nexusfin.equity.dto.request.BankCardSignConfirmRequest;
import com.nexusfin.equity.dto.response.BankCardSignApplyResponse;
import com.nexusfin.equity.dto.response.BankCardSignConfirmResponse;
import com.nexusfin.equity.dto.response.BankCardSignStatusResponse;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.BankCardSignServiceImpl;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwSignApplyResponse;
import com.nexusfin.equity.thirdparty.qw.QwSignConfirmResponse;
import com.nexusfin.equity.thirdparty.qw.QwSignStatusResponse;
import com.nexusfin.equity.util.SensitiveDataCipher;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
class BankCardSignServiceTest {

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private MemberChannelRepository memberChannelRepository;

    @Mock
    private SensitiveDataCipher sensitiveDataCipher;

    @Mock
    private QwBenefitClient qwBenefitClient;

    @Mock
    private PaymentProtocolService paymentProtocolService;

    @InjectMocks
    private BankCardSignServiceImpl bankCardSignService;

    @Test
    void shouldReturnSignedStatusWhenQwReportsSigned(CapturedOutput output) {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(qwBenefitClient.querySignStatus(any())).thenReturn(new QwSignStatusResponse(1));

        BankCardSignStatusResponse response = bankCardSignService.getSignStatus("mem-1", "6222020202020208");

        assertThat(response.signed()).isTrue();
        assertThat(response.status()).isEqualTo("SIGNED");
        assertThat(response.accountNo()).isEqualTo("6222020202020208");
        assertThat(output).contains("bank-card sign status qw request begin");
        assertThat(output).contains("bank-card sign status qw request success");
        assertThat(output).contains("bizOrderNo=bank-card-0208");
        assertThat(output).doesNotContain("6222020202020208");
    }

    @Test
    void shouldApplySignUsingMemberSensitiveFields(CapturedOutput output) {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(qwBenefitClient.applySign(any())).thenReturn(new QwSignApplyResponse("qw-apply-001"));

        BankCardSignApplyResponse response = bankCardSignService.applySign(
                "mem-1",
                new BankCardSignApplyRequest("6222020202021234")
        );

        assertThat(response.requestNo()).isEqualTo("qw-apply-001");
        assertThat(response.status()).isEqualTo("SMS_SENT");
        assertThat(output).contains("bank-card sign apply qw request begin");
        assertThat(output).contains("bank-card sign apply qw request success");
        assertThat(output).contains("bizOrderNo=bank-card-1234");
        assertThat(output).contains("requestNo=qw-apply-001");
        assertThat(output).doesNotContain("6222020202021234");
    }

    @Test
    void shouldPersistLocalProtocolWhenSignConfirmSucceeds(CapturedOutput output) {
        MemberInfo memberInfo = buildMember();
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setMemberId("mem-1");
        memberChannel.setChannelCode("KJ");
        memberChannel.setExternalUserId("tech-user-1");
        memberChannel.setCreatedTs(LocalDateTime.now());
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(memberChannelRepository.selectLatestByMemberId("mem-1")).thenReturn(memberChannel);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(qwBenefitClient.confirmSign(any())).thenReturn(new QwSignConfirmResponse("qw-confirm-001", "ACTIVE"));

        BankCardSignConfirmResponse response = bankCardSignService.confirmSign(
                "mem-1",
                new BankCardSignConfirmRequest("6222020202025678", "123456")
        );

        assertThat(response.signed()).isTrue();
        assertThat(response.status()).isEqualTo("SIGNED");
        assertThat(response.requestNo()).isEqualTo("qw-confirm-001");

        ArgumentCaptor<PaymentProtocolService.SavePaymentProtocolCommand> captor =
                ArgumentCaptor.forClass(PaymentProtocolService.SavePaymentProtocolCommand.class);
        verify(paymentProtocolService).saveActiveProtocol(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo("mem-1");
        assertThat(captor.getValue().externalUserId()).isEqualTo("tech-user-1");
        assertThat(captor.getValue().providerCode()).isEqualTo("QW_SIGN");
        assertThat(captor.getValue().protocolNo()).isEqualTo("QW-SIGN-qw-confirm-001");
        assertThat(captor.getValue().signRequestNo()).isEqualTo("qw-confirm-001");
        assertThat(captor.getValue().channelCode()).isEqualTo("KJ");
        assertThat(output).contains("bank-card sign confirm qw request begin");
        assertThat(output).contains("bank-card sign confirm qw request success");
        assertThat(output).contains("bizOrderNo=bank-card-5678");
        assertThat(output).contains("requestNo=qw-confirm-001");
        assertThat(output).doesNotContain("6222020202025678");
        assertThat(output).doesNotContain("123456");
    }

    @Test
    void shouldLogWarnWhenSignConfirmRejected(CapturedOutput output) {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(qwBenefitClient.confirmSign(any())).thenReturn(new QwSignConfirmResponse("qw-confirm-002", "FAILED"));

        assertThatThrownBy(() -> bankCardSignService.confirmSign(
                "mem-1",
                new BankCardSignConfirmRequest("6222020202025678", "123456")
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("QW sign confirm failed");

        assertThat(output).contains("bank-card sign confirm qw request begin");
        assertThat(output).contains("errorNo=QW_SIGN_CONFIRM_FAILED");
        assertThat(output).contains("errorMsg=QW sign confirm failed");
        assertThat(output).contains("bizOrderNo=bank-card-5678");
        assertThat(output).doesNotContain("6222020202025678");
        assertThat(output).doesNotContain("123456");
    }

    @Test
    void shouldRejectWhenMemberMissing() {
        when(memberInfoRepository.selectById("mem-missing")).thenReturn(null);

        assertThatThrownBy(() -> bankCardSignService.getSignStatus("mem-missing", "6222020202020208"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Member not found");
    }

    private MemberInfo buildMember() {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId("mem-1");
        memberInfo.setExternalUserId("tech-user-1");
        memberInfo.setTechPlatformUserId("tech-user-1");
        memberInfo.setMobileEncrypted("mobile-cipher");
        memberInfo.setRealNameEncrypted("name-cipher");
        memberInfo.setIdCardEncrypted("id-cipher");
        return memberInfo;
    }
}
