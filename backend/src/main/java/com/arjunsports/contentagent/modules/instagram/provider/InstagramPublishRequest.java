package com.arjunsports.contentagent.modules.instagram.provider;

import lombok.Builder;

@Builder
public record InstagramPublishRequest(
        String businessAccountId,
        String accessToken,
        String imageUrl,
        String caption) {
}
