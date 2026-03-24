package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.CurrentUserResponse;

public interface AuthService {

    AuthLoginResult loginWithTechToken(String techToken, String redirectUrl);

    CurrentUserResponse getCurrentUser();

    record AuthLoginResult(
            String redirectUrl,
            String jwtToken
    ) {
    }
}
