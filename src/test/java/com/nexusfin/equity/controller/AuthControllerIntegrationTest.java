package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.response.TechPlatformUserProfileResponse;
import com.nexusfin.equity.service.TechPlatformUserClient;
import com.nexusfin.equity.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "classpath:db/test-data.sql")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private TechPlatformUserClient techPlatformUserClient;

    @Test
    void shouldCreateCookieAndRedirectForSsoCallback() throws Exception {
        when(techPlatformUserClient.getCurrentUser("tech-token")).thenReturn(
                new TechPlatformUserProfileResponse("tech-user-sso-001", "13800138000", "张三", "310101199001011111")
        );

        mockMvc.perform(get("/api/auth/sso-callback")
                        .param("token", "tech-token")
                        .param("redirect_url", "/equity/index"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/equity/index"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("NEXUSFIN_AUTH=")));
    }

    @Test
    void shouldReturnCurrentUserWhenJwtCookieIsPresent() throws Exception {
        String jwt = jwtUtil.generateToken("mem-auth-seed-001", "tech-user-seed-001");

        mockMvc.perform(get("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("NEXUSFIN_AUTH", jwt))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.memberId").value("mem-auth-seed-001"))
                .andExpect(jsonPath("$.data.techPlatformUserId").value("tech-user-seed-001"));
    }

    @Test
    void shouldRejectCurrentUserWhenJwtCookieMissing() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectInvalidRedirectUrl() throws Exception {
        when(techPlatformUserClient.getCurrentUser("tech-token")).thenReturn(
                new TechPlatformUserProfileResponse("tech-user-sso-002", "13800138001", "李四", "310101199001011112")
        );

        mockMvc.perform(get("/api/auth/sso-callback")
                        .param("token", "tech-token")
                        .param("redirect_url", "https://evil.example.com/hijack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("REDIRECT_URL_INVALID")));
    }
}
