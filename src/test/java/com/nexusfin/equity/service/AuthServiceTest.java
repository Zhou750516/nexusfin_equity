package com.nexusfin.equity.service;

import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.dto.response.CurrentUserResponse;
import com.nexusfin.equity.dto.response.TechPlatformUserProfileResponse;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.AuthServiceImpl;
import com.nexusfin.equity.util.AuthContextUtil;
import com.nexusfin.equity.util.AuthPrincipal;
import com.nexusfin.equity.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TechPlatformUserClient techPlatformUserClient;

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private MemberChannelRepository memberChannelRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void tearDown() {
        AuthContextUtil.clear();
    }

    @Test
    void shouldProvisionMemberAndIssueJwtForSsoLogin() {
        AuthProperties authProperties = authProperties();
        authService = new AuthServiceImpl(
                techPlatformUserClient,
                memberInfoRepository,
                memberChannelRepository,
                new JwtUtil(authProperties),
                authProperties
        );
        when(techPlatformUserClient.getCurrentUser("tech-token")).thenReturn(
                new TechPlatformUserProfileResponse("tech-001", "13800138000", "张三", "310101199001011111")
        );
        when(memberInfoRepository.selectByTechPlatformUserId("tech-001")).thenReturn(null);
        when(memberChannelRepository.selectByChannelAndExternalUserId("KJ", "tech-001")).thenReturn(null);

        AuthService.AuthLoginResult result = authService.loginWithTechToken("tech-token", "/equity/index");

        ArgumentCaptor<MemberInfo> memberCaptor = ArgumentCaptor.forClass(MemberInfo.class);
        verify(memberInfoRepository).insert(memberCaptor.capture());
        verify(memberChannelRepository).insert(any(MemberChannel.class));
        assertThat(memberCaptor.getValue().getTechPlatformUserId()).isEqualTo("tech-001");
        assertThat(result.redirectUrl()).isEqualTo("/equity/index");
        assertThat(result.jwtToken()).isNotBlank();
    }

    @Test
    void shouldReuseExistingMemberForRepeatSsoLogin() {
        AuthProperties authProperties = authProperties();
        authService = new AuthServiceImpl(
                techPlatformUserClient,
                memberInfoRepository,
                memberChannelRepository,
                new JwtUtil(authProperties),
                authProperties
        );
        MemberInfo existing = new MemberInfo();
        existing.setMemberId("mem-existing");
        existing.setTechPlatformUserId("tech-001");
        when(techPlatformUserClient.getCurrentUser("tech-token")).thenReturn(
                new TechPlatformUserProfileResponse("tech-001", "13800138000", "张三", null)
        );
        when(memberInfoRepository.selectByTechPlatformUserId("tech-001")).thenReturn(existing);
        when(memberChannelRepository.selectByChannelAndExternalUserId("KJ", "tech-001")).thenReturn(new MemberChannel());

        AuthService.AuthLoginResult result = authService.loginWithTechToken("tech-token", null);

        verify(memberInfoRepository, never()).insert(any());
        verify(memberInfoRepository).updateById(existing);
        assertThat(result.redirectUrl()).isEqualTo("/equity/index");
    }

    @Test
    void shouldReturnCurrentUserFromAuthContext() {
        AuthProperties authProperties = authProperties();
        authService = new AuthServiceImpl(
                techPlatformUserClient,
                memberInfoRepository,
                memberChannelRepository,
                new JwtUtil(authProperties),
                authProperties
        );
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId("mem-001");
        memberInfo.setTechPlatformUserId("tech-001");
        memberInfo.setExternalUserId("tech-001");
        memberInfo.setMemberStatus("ACTIVE");
        when(memberInfoRepository.selectById("mem-001")).thenReturn(memberInfo);
        AuthContextUtil.bind(new AuthPrincipal("mem-001", "tech-001"));

        CurrentUserResponse response = authService.getCurrentUser();

        assertThat(response.memberId()).isEqualTo("mem-001");
        assertThat(response.techPlatformUserId()).isEqualTo("tech-001");
    }

    private AuthProperties authProperties() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setDefaultChannelCode("KJ");
        authProperties.setDefaultRedirectUrl("/equity/index");
        authProperties.getRedirectWhitelist().add("/equity/");
        AuthProperties.Jwt jwt = new AuthProperties.Jwt();
        jwt.setIssuer("test-issuer");
        jwt.setSecret("test-jwt-secret-key-test-jwt-secret-key");
        authProperties.setJwt(jwt);
        return authProperties;
    }
}
