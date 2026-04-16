package com.nexusfin.equity.service;

import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.config.CryptoProperties;
import com.nexusfin.equity.dto.request.JointLoginRequest;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.JointLoginServiceImpl;
import com.nexusfin.equity.util.JointLoginTargetPageResolver;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JointLoginServiceTest {

    private final SensitiveDataCipher sensitiveDataCipher = new SensitiveDataCipher(new CryptoProperties());

    @Mock
    private XiaohuaAuthClient xiaohuaAuthClient;

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private MemberChannelRepository memberChannelRepository;

    @Test
    void shouldCreateJwtAndResolveDispatchPageForPushScene() {
        AuthProperties authProperties = authProperties();
        JointLoginServiceImpl jointLoginService = new JointLoginServiceImpl(
                xiaohuaAuthClient,
                memberInfoRepository,
                memberChannelRepository,
                new JwtUtil(authProperties),
                authProperties,
                sensitiveDataCipher,
                new JointLoginTargetPageResolver()
        );
        JointLoginRequest request = new JointLoginRequest(
                "joint-token-001",
                "push",
                null,
                "BEN-20260417-001",
                "HUXUAN_CARD"
        );

        when(xiaohuaAuthClient.exchange("joint-token-001"))
                .thenReturn(new XiaohuaAuthClient.JointIdentity(
                        "xh-user-001",
                        "13800138000",
                        "张三",
                        "310101199001011111",
                        "KJ"
                ));
        when(memberInfoRepository.selectByExternalUserId("xh-user-001")).thenReturn(null);
        when(memberChannelRepository.selectByChannelAndExternalUserId("KJ", "xh-user-001")).thenReturn(null);

        JointLoginService.JointLoginResult result = jointLoginService.login(request);

        ArgumentCaptor<MemberInfo> memberCaptor = ArgumentCaptor.forClass(MemberInfo.class);
        verify(memberInfoRepository).insert(memberCaptor.capture());
        verify(memberChannelRepository).insert(any(MemberChannel.class));
        assertThat(memberCaptor.getValue().getExternalUserId()).isEqualTo("xh-user-001");
        assertThat(memberCaptor.getValue().getMobileEncrypted()).isNotEqualTo("13800138000");
        assertThat(result.jwtToken()).isNotBlank();
        assertThat(result.scene()).isEqualTo("push");
        assertThat(result.targetPage()).isEqualTo("joint-dispatch");
        assertThat(result.benefitOrderNo()).isEqualTo("BEN-20260417-001");
        assertThat(result.localUserReady()).isTrue();
    }

    @Test
    void shouldReuseExistingMemberForRepeatJointLogin() {
        AuthProperties authProperties = authProperties();
        JointLoginServiceImpl jointLoginService = new JointLoginServiceImpl(
                xiaohuaAuthClient,
                memberInfoRepository,
                memberChannelRepository,
                new JwtUtil(authProperties),
                authProperties,
                sensitiveDataCipher,
                new JointLoginTargetPageResolver()
        );
        MemberInfo existing = new MemberInfo();
        existing.setMemberId("mem-existing");
        existing.setExternalUserId("xh-user-001");
        existing.setCreatedTs(java.time.LocalDateTime.now().minusDays(1));
        JointLoginRequest request = new JointLoginRequest(
                "joint-token-002",
                "refund",
                null,
                "BEN-20260417-002",
                null
        );

        when(xiaohuaAuthClient.exchange("joint-token-002"))
                .thenReturn(new XiaohuaAuthClient.JointIdentity(
                        "xh-user-001",
                        "13800138000",
                        "张三",
                        null,
                        "KJ"
                ));
        when(memberInfoRepository.selectByExternalUserId("xh-user-001")).thenReturn(existing);
        when(memberChannelRepository.selectByChannelAndExternalUserId("KJ", "xh-user-001")).thenReturn(new MemberChannel());

        JointLoginService.JointLoginResult result = jointLoginService.login(request);

        verify(memberInfoRepository, never()).insert(any());
        verify(memberInfoRepository).updateById(existing);
        assertThat(result.targetPage()).isEqualTo("joint-refund-entry");
        assertThat(result.scene()).isEqualTo("refund");
    }

    private AuthProperties authProperties() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setDefaultChannelCode("KJ");
        AuthProperties.Jwt jwt = new AuthProperties.Jwt();
        jwt.setIssuer("test-issuer");
        jwt.setSecret("test-jwt-secret-key-test-jwt-secret-key");
        authProperties.setJwt(jwt);
        return authProperties;
    }
}
