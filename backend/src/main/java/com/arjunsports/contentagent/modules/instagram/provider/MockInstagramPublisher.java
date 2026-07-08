package com.arjunsports.contentagent.modules.instagram.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stand-in for the real Meta Graph API, used so the Instagram Publisher can
 * be developed, demoed, and tested end-to-end without production Meta
 * credentials. Only ever registered when the {@code dev} profile is active -
 * it does not exist as a bean at all under {@code prod} (see
 * {@link com.arjunsports.contentagent.modules.ai.provider.MockAIProvider}
 * for the identical rationale applied to the AI provider in Module 3).
 *
 * <p>Include the token {@code SIMULATE_FAILURE} in the caption to deliberately
 * exercise the failure path in dev.
 */
@Slf4j
@Component
@Profile("dev")
public class MockInstagramPublisher implements InstagramPublisher {

    private static final String PUBLISHER_NAME = "mock";
    private static final String FAILURE_TRIGGER = "SIMULATE_FAILURE";

    @Override
    public String getPublisherName() {
        return PUBLISHER_NAME;
    }

    @Override
    public InstagramAccountInfo verifyAccount(String businessAccountId, String accessToken) {
        simulateLatency();
        log.info("MockInstagramPublisher verified dev account {}", businessAccountId);
        return InstagramAccountInfo.builder()
                .businessAccountId(businessAccountId)
                .username("mock_academy_account")
                .build();
    }

    @Override
    public InstagramPublishResult publish(InstagramPublishRequest request) {
        if (request.caption() != null && request.caption().contains(FAILURE_TRIGGER)) {
            throw new InstagramPublisherException(InstagramPublisherException.Reason.UNAVAILABLE,
                    "Mock publisher: simulated failure (" + FAILURE_TRIGGER + " marker present in caption)");
        }

        simulateLatency();
        String mockMediaId = "mock_media_" + UUID.randomUUID();
        log.info("MockInstagramPublisher published a dev post as {}", mockMediaId);

        return InstagramPublishResult.builder()
                .instagramMediaId(mockMediaId)
                .permalink("https://www.instagram.com/p/" + mockMediaId.substring(0, 11) + "/")
                .build();
    }

    private void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(300, 900));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
