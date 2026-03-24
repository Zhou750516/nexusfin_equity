package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.TechPlatformUserProfileResponse;

public interface TechPlatformUserClient {

    TechPlatformUserProfileResponse getCurrentUser(String techToken);
}
