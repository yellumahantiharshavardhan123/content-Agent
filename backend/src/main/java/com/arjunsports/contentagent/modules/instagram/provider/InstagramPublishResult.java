package com.arjunsports.contentagent.modules.instagram.provider;

import lombok.Builder;

@Builder
public record InstagramPublishResult(String instagramMediaId, String permalink) {
}
