package com.nexusfin.equity.service;

import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.config.CryptoProperties;
import com.nexusfin.equity.dto.request.JointLoginRequest;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.JointLoginServiceImpl;
import com.nexusfin.equity.thirdparty.yunka.UserQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.UserQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserTokenRequest;
import com.nexusfin.equity.thirdparty.yunka.UserTokenResponse;
import com.nexusfin.equity.util.JointLoginTargetPageResolver;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class JointLoginServiceTest {

    private final SensitiveDataCipher sensitiveDataCipher = new SensitiveDataCipher(new CryptoProperties());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private XiaohuaGatewayService xiaohuaGatewayService;

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private MemberChannelRepository memberChannelRepository;

    @Test
    void shouldCreateJwtAndResolveDispatchPageForPushScene() throws Exception {
        AuthProperties authProperties = authProperties();
        JointLoginServiceImpl jointLoginService = new JointLoginServiceImpl(
                xiaohuaGatewayService,
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

        when(xiaohuaGatewayService.validateUserToken(any(), any(), any()))
                .thenReturn(new UserTokenResponse("xh-cid-001", "张三", "13800138000"));
        when(xiaohuaGatewayService.queryUser(any(), any(), any()))
                .thenReturn(new UserQueryResponse(objectMapper.readTree("""
                        {
                          "idInfo": {
                            "idno": "310101199001011111"
                          }
                        }
                        """)));
        when(memberInfoRepository.selectByExternalUserId("xh-cid-001")).thenReturn(null);
        when(memberChannelRepository.selectByChannelAndExternalUserId("KJ", "xh-cid-001")).thenReturn(null);

        JointLoginService.JointLoginResult result = jointLoginService.login(request);

        ArgumentCaptor<MemberInfo> memberCaptor = ArgumentCaptor.forClass(MemberInfo.class);
        verify(memberInfoRepository).insert(memberCaptor.capture());
        verify(memberChannelRepository).insert(any(MemberChannel.class));
        verify(xiaohuaGatewayService).validateUserToken(any(), any(), argThat((UserTokenRequest tokenRequest) ->
                tokenRequest.userId() == null && "joint-token-001".equals(tokenRequest.token())));
        verify(xiaohuaGatewayService).queryUser(any(), any(), argThat((UserQueryRequest userQueryRequest) ->
                userQueryRequest.userId() != null
                        && userQueryRequest.userId().startsWith("mem")
                        && "xh-cid-001".equals(userQueryRequest.cid())));
        assertThat(memberCaptor.getValue().getExternalUserId()).isEqualTo("xh-cid-001");
        assertThat(memberCaptor.getValue().getMobileEncrypted()).isNotEqualTo("13800138000");
        assertThat(result.jwtToken()).isNotBlank();
        assertThat(result.scene()).isEqualTo("push");
        assertThat(result.targetPage()).isEqualTo("joint-dispatch");
        assertThat(result.benefitOrderNo()).isEqualTo("BEN-20260417-001");
        assertThat(result.localUserReady()).isTrue();
    }

    @Test
    void shouldReuseExistingMemberForRepeatJointLogin() throws Exception {
        AuthProperties authProperties = authProperties();
        JointLoginServiceImpl jointLoginService = new JointLoginServiceImpl(
                xiaohuaGatewayService,
                memberInfoRepository,
                memberChannelRepository,
                new JwtUtil(authProperties),
                authProperties,
                sensitiveDataCipher,
                new JointLoginTargetPageResolver()
        );
        MemberInfo existing = new MemberInfo();
        existing.setMemberId("mem-existing");
        existing.setExternalUserId("xh-cid-001");
        existing.setCreatedTs(java.time.LocalDateTime.now().minusDays(1));
        JointLoginRequest request = new JointLoginRequest(
                "joint-token-002",
                "refund",
                null,
                "BEN-20260417-002",
                null
        );

        when(xiaohuaGatewayService.validateUserToken(any(), any(), any()))
                .thenReturn(new UserTokenResponse("xh-cid-001", "张三", "13800138000"));
        when(xiaohuaGatewayService.queryUser(any(), any(), any()))
                .thenReturn(new UserQueryResponse(objectMapper.readTree("""
                        {
                          "idInfo": {
                            "idno": "310101199001011111"
                          }
                        }
                        """)));
        when(memberInfoRepository.selectByExternalUserId("xh-cid-001")).thenReturn(existing);
        when(memberChannelRepository.selectByChannelAndExternalUserId("KJ", "xh-cid-001")).thenReturn(new MemberChannel());

        JointLoginService.JointLoginResult result = jointLoginService.login(request);

        verify(memberInfoRepository, never()).insert(any());
        verify(memberInfoRepository).updateById(existing);
        verify(xiaohuaGatewayService).queryUser(any(), any(), argThat((UserQueryRequest userQueryRequest) ->
                "mem-existing".equals(userQueryRequest.userId())
                        && "xh-cid-001".equals(userQueryRequest.cid())));
        assertThat(result.targetPage()).isEqualTo("joint-refund-entry");
        assertThat(result.scene()).isEqualTo("refund");
    }

    @Test
    void shouldTranslateRejectedTokenToJointLoginSessionExpired() {
        AuthProperties authProperties = authProperties();
        JointLoginServiceImpl jointLoginService = new JointLoginServiceImpl(
                xiaohuaGatewayService,
                memberInfoRepository,
                memberChannelRepository,
                new JwtUtil(authProperties),
                authProperties,
                sensitiveDataCipher,
                new JointLoginTargetPageResolver()
        );
        JointLoginRequest request = new JointLoginRequest(
                "joint-token-003",
                "push",
                null,
                "BEN-20260417-003",
                null
        );

        when(xiaohuaGatewayService.validateUserToken(any(), any(), any()))
                .thenThrow(new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, "token invalid"));

        assertThatThrownBy(() -> jointLoginService.login(request))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("JOINT_LOGIN_TOKEN_INVALID");
    }

    @Test
    void shouldTranslateTimeoutToJointLoginUpstreamTimeout() {
        AuthProperties authProperties = authProperties();
        JointLoginServiceImpl jointLoginService = new JointLoginServiceImpl(
                xiaohuaGatewayService,
                memberInfoRepository,
                memberChannelRepository,
                new JwtUtil(authProperties),
                authProperties,
                sensitiveDataCipher,
                new JointLoginTargetPageResolver()
        );
        JointLoginRequest request = new JointLoginRequest(
                "joint-token-004",
                "push",
                null,
                "BEN-20260417-004",
                null
        );

        when(xiaohuaGatewayService.validateUserToken(any(), any(), any()))
                .thenThrow(new UpstreamTimeoutException("Yunka gateway timeout"));

        assertThatThrownBy(() -> jointLoginService.login(request))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("JOINT_LOGIN_UPSTREAM_TIMEOUT");
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
