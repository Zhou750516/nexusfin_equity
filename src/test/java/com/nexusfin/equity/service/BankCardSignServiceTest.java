package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.BankCardSignApplyRequest;
import com.nexusfin.equity.dto.request.BankCardSignConfirmRequest;
import com.nexusfin.equity.dto.response.BankCardSignApplyResponse;
import com.nexusfin.equity.dto.response.BankCardSignConfirmResponse;
import com.nexusfin.equity.dto.response.BankCardSignStatusResponse;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.BankCardSignServiceImpl;
import com.nexusfin.equity.config.QwDirectProperties;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwSignApplyRequest;
import com.nexusfin.equity.thirdparty.qw.QwSignApplyResponse;
import com.nexusfin.equity.thirdparty.qw.QwSignConfirmRequest;
import com.nexusfin.equity.thirdparty.qw.QwSignConfirmResponse;
import com.nexusfin.equity.thirdparty.qw.QwSignStatusRequest;
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
import static org.mockito.Mockito.never;
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

    @Mock
    private QwProperties qwProperties;

    @Mock
    private QwDirectProperties qwDirectProperties;

    @InjectMocks
    private BankCardSignServiceImpl bankCardSignService;

    @Test
    void shouldReturnSignedStatusWhenQwReportsSigned(CapturedOutput output) {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(qwProperties.getDirect()).thenReturn(qwDirectProperties);
        when(qwDirectProperties.getMerchantId()).thenReturn("200000000007804");
        when(qwBenefitClient.querySignStatus(any())).thenReturn(new QwSignStatusResponse(1));

        BankCardSignStatusResponse response = bankCardSignService.getSignStatus("mem-1", "6222020202020208");

        ArgumentCaptor<QwSignStatusRequest> requestCaptor = ArgumentCaptor.forClass(QwSignStatusRequest.class);
        verify(qwBenefitClient).querySignStatus(requestCaptor.capture());
        assertThat(requestCaptor.getValue().merchantId()).isEqualTo("200000000007804");
        assertThat(requestCaptor.getValue().phone()).isEqualTo("13800138000");
        assertThat(requestCaptor.getValue().name()).isEqualTo("测试用户");
        assertThat(requestCaptor.getValue().accountNo()).isEqualTo("6222020202020208");
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
        when(qwProperties.getDirect()).thenReturn(qwDirectProperties);
        when(qwDirectProperties.getMerchantId()).thenReturn("200000000007804");
        when(qwBenefitClient.applySign(any())).thenReturn(new QwSignApplyResponse(88001234L, "2026-04-29 10:00:00"));

        BankCardSignApplyResponse response = bankCardSignService.applySign(
                "mem-1",
                new BankCardSignApplyRequest("6222020202021234")
        );

        ArgumentCaptor<QwSignApplyRequest> requestCaptor = ArgumentCaptor.forClass(QwSignApplyRequest.class);
        verify(qwBenefitClient).applySign(requestCaptor.capture());
        assertThat(requestCaptor.getValue().merchantId()).isEqualTo("200000000007804");
        assertThat(requestCaptor.getValue().phone()).isEqualTo("13800138000");
        assertThat(requestCaptor.getValue().name()).isEqualTo("测试用户");
        assertThat(requestCaptor.getValue().accountNo()).isEqualTo("6222020202021234");
        assertThat(requestCaptor.getValue().idNo()).isEqualTo("110101199003071234");
        assertThat(response.userSignId()).isEqualTo(88001234L);
        assertThat(response.applyTime()).isEqualTo("2026-04-29 10:00:00");
        assertThat(response.status()).isEqualTo("SMS_SENT");
        assertThat(output).contains("bank-card sign apply qw request begin");
        assertThat(output).contains("bank-card sign apply qw request success");
        assertThat(output).contains("bizOrderNo=bank-card-1234");
        assertThat(output).contains("userSignId=88001234");
        assertThat(output).doesNotContain("6222020202021234");
    }

    @Test
    void shouldTranslateApplySignTimeoutToBizException(CapturedOutput output) {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(qwProperties.getDirect()).thenReturn(qwDirectProperties);
        when(qwDirectProperties.getMerchantId()).thenReturn("200000000007804");
        when(qwBenefitClient.applySign(any()))
                .thenThrow(new UpstreamTimeoutException("Mock QW timeout: 6222020202021234_FAULT_TIMEOUT"));

        assertThatThrownBy(() -> bankCardSignService.applySign(
                "mem-1",
                new BankCardSignApplyRequest("6222020202021234_FAULT_TIMEOUT")
        ))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("QW_SIGN_UPSTREAM_TIMEOUT");

        assertThat(output).contains("bank-card sign apply qw request begin");
        assertThat(output).contains("bank-card sign apply qw request failed");
        assertThat(output).contains("errorNo=QW_SIGN_UPSTREAM_FAILED");
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
        when(qwBenefitClient.confirmSign(any())).thenReturn(new QwSignConfirmResponse(5678L, "AGRM-5678"));

        BankCardSignConfirmResponse response = bankCardSignService.confirmSign(
                "mem-1",
                new BankCardSignConfirmRequest(5678L, "123456")
        );

        ArgumentCaptor<QwSignConfirmRequest> requestCaptor = ArgumentCaptor.forClass(QwSignConfirmRequest.class);
        verify(qwBenefitClient).confirmSign(requestCaptor.capture());
        assertThat(requestCaptor.getValue().userSignId()).isEqualTo(5678L);
        assertThat(requestCaptor.getValue().verCode()).isEqualTo("123456");
        assertThat(response.signed()).isTrue();
        assertThat(response.status()).isEqualTo("SIGNED");
        assertThat(response.userSignId()).isEqualTo(5678L);
        assertThat(response.agreementNo()).isEqualTo("AGRM-5678");

        ArgumentCaptor<PaymentProtocolService.SavePaymentProtocolCommand> captor =
                ArgumentCaptor.forClass(PaymentProtocolService.SavePaymentProtocolCommand.class);
        verify(paymentProtocolService).saveActiveProtocol(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo("mem-1");
        assertThat(captor.getValue().externalUserId()).isEqualTo("tech-user-1");
        assertThat(captor.getValue().providerCode()).isEqualTo("QW_SIGN");
        assertThat(captor.getValue().protocolNo()).isEqualTo("AGRM-5678");
        assertThat(captor.getValue().signRequestNo()).isEqualTo("5678");
        assertThat(captor.getValue().channelCode()).isEqualTo("KJ");
        assertThat(output).contains("bank-card sign confirm qw request begin");
        assertThat(output).contains("bank-card sign confirm qw request success");
        assertThat(output).contains("bizOrderNo=bank-card-sign-5678");
        assertThat(output).contains("userSignId=5678");
        assertThat(output).contains("agreementNo=AGRM-5678");
        assertThat(output).doesNotContain("123456");
    }

    @Test
    void shouldLogWarnWhenSignConfirmRejected(CapturedOutput output) {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(qwBenefitClient.confirmSign(any())).thenReturn(new QwSignConfirmResponse(5678L, ""));

        assertThatThrownBy(() -> bankCardSignService.confirmSign(
                "mem-1",
                new BankCardSignConfirmRequest(5678L, "123456")
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("QW sign confirm failed");

        assertThat(output).contains("bank-card sign confirm qw request begin");
        assertThat(output).contains("errorNo=QW_SIGN_CONFIRM_FAILED");
        assertThat(output).contains("errorMsg=QW sign confirm failed");
        assertThat(output).contains("bizOrderNo=bank-card-sign-5678");
        assertThat(output).doesNotContain("123456");
    }

    @Test
    void shouldTranslateConfirmSignTimeoutToBizException(CapturedOutput output) {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(qwBenefitClient.confirmSign(any()))
                .thenThrow(new UpstreamTimeoutException("Mock QW timeout: REQ_EX_P0_1_3_FAULT_TIMEOUT"));

        assertThatThrownBy(() -> bankCardSignService.confirmSign(
                "mem-1",
                new BankCardSignConfirmRequest(5678L, "123456")
        ))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("QW_SIGN_UPSTREAM_TIMEOUT");

        verify(paymentProtocolService, never()).saveActiveProtocol(any());
        assertThat(output).contains("bank-card sign confirm qw request begin");
        assertThat(output).contains("bank-card sign confirm qw request failed");
        assertThat(output).contains("errorNo=QW_SIGN_UPSTREAM_FAILED");
        assertThat(output).doesNotContain("123456");
    }

    @Test
    void shouldRejectWhenMemberMissing() {
        when(memberInfoRepository.selectById("mem-missing")).thenReturn(null);

        assertThatThrownBy(() -> bankCardSignService.getSignStatus("mem-missing", "6222020202020208"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Member not found");
    }

    @Test
    void shouldTranslateSignStatusTimeoutToBizException(CapturedOutput output) {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(qwProperties.getDirect()).thenReturn(qwDirectProperties);
        when(qwDirectProperties.getMerchantId()).thenReturn("200000000007804");
        when(qwBenefitClient.querySignStatus(any()))
                .thenThrow(new UpstreamTimeoutException("Mock QW timeout: 6222020202021234_FAULT_TIMEOUT"));

        assertThatThrownBy(() -> bankCardSignService.getSignStatus("mem-1", "6222020202021234_FAULT_TIMEOUT"))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("QW_SIGN_UPSTREAM_TIMEOUT");

        assertThat(output).contains("bank-card sign status qw request begin");
        assertThat(output).contains("bank-card sign status qw request failed");
        assertThat(output).contains("errorNo=QW_SIGN_UPSTREAM_FAILED");
    }

    @Test
    void shouldFailFastWhenMerchantIdMissingOutsideMockMode() {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(qwProperties.getMode()).thenReturn(QwProperties.Mode.ALLINPAY_DIRECT);
        when(qwProperties.getDirect()).thenReturn(qwDirectProperties);
        when(qwDirectProperties.getMerchantId()).thenReturn(null);

        assertThatThrownBy(() -> bankCardSignService.getSignStatus("mem-1", "6222020202020208"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("QW_SIGN_MERCHANT_ID_MISSING");

        verify(qwBenefitClient, never()).querySignStatus(any());
    }

    @Test
    void shouldAllowMissingMerchantIdInMockMode() {
        MemberInfo memberInfo = buildMember();
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("测试用户");
        when(sensitiveDataCipher.decrypt("id-cipher")).thenReturn("110101199003071234");
        when(qwProperties.getMode()).thenReturn(QwProperties.Mode.MOCK);
        when(qwBenefitClient.applySign(any())).thenReturn(new QwSignApplyResponse(88001234L, "2026-04-29 10:00:00"));

        BankCardSignApplyResponse response = bankCardSignService.applySign(
                "mem-1",
                new BankCardSignApplyRequest("6222020202021234")
        );

        ArgumentCaptor<QwSignApplyRequest> requestCaptor = ArgumentCaptor.forClass(QwSignApplyRequest.class);
        verify(qwBenefitClient).applySign(requestCaptor.capture());
        assertThat(requestCaptor.getValue().merchantId()).isNull();
        assertThat(response.userSignId()).isEqualTo(88001234L);
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
