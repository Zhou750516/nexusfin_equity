package com.nexusfin.equity.controller;

import com.nexusfin.equity.service.JointLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JointLoginControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JointLoginService jointLoginService;

    @Test
    void shouldCreateCookieAndReturnTargetPageForJointLogin() throws Exception {
        when(jointLoginService.login(any())).thenReturn(
                new JointLoginService.JointLoginResult(
                        "jwt-joint-token",
                        "push",
                        "joint-dispatch",
                        "BEN-20260417-001",
                        true
                )
        );

        mockMvc.perform(post("/api/auth/joint-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "joint-token-001",
                                  "scene": "push",
                                  "benefitOrderNo": "BEN-20260417-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scene").value("push"))
                .andExpect(jsonPath("$.data.targetPage").value("joint-dispatch"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("NEXUSFIN_AUTH=")));
    }
}
