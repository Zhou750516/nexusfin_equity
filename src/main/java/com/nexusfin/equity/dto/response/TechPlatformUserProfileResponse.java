package com.nexusfin.equity.dto.response;

public record TechPlatformUserProfileResponse(
        String userId,
        String phone,
        String realName,
        String idCard,
        String channelCode
) {
}
