package com.nexusfin.equity.dto.response;

public record CurrentUserResponse(
        String memberId,
        String techPlatformUserId,
        String externalUserId,
        String memberStatus
) {
}
