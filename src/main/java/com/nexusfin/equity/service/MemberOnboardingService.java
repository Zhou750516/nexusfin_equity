package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.RegisterUserRequest;
import com.nexusfin.equity.dto.response.RegisterUserResponse;

public interface MemberOnboardingService {

    RegisterUserResponse register(RegisterUserRequest request);
}
