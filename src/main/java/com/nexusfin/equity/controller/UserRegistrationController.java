package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.RegisterUserRequest;
import com.nexusfin.equity.dto.response.RegisterUserResponse;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.MemberOnboardingService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserRegistrationController {

    private final MemberOnboardingService memberOnboardingService;

    public UserRegistrationController(MemberOnboardingService memberOnboardingService) {
        this.memberOnboardingService = memberOnboardingService;
    }

    @PostMapping("/register")
    public Result<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return Result.success(memberOnboardingService.register(request));
    }
}
