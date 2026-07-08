package com.arjunsports.contentagent.modules.instagram.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/** Never carries the access token - that value never leaves the service layer once encrypted. */
@Builder
public record InstagramAccountResponse(
        UUID id,
        boolean connected,
        String businessAccountId,
        String facebookPageId,
        String username,
        Instant connectedAt,
        Instant disconnectedAt) {

    public static InstagramAccountResponse notConnected() {
        return InstagramAccountResponse.builder().connected(false).build();
    }
}
