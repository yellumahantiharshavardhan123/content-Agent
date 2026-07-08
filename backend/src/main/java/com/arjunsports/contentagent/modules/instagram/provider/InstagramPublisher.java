package com.arjunsports.contentagent.modules.instagram.provider;

/**
 * Abstraction over an Instagram publishing backend. The real Meta Graph API
 * implementation and a dev-only mock are both plain implementations of this
 * interface - callers (InstagramServiceImpl) never branch on which one is
 * active, so switching is purely a configuration change (app.instagram.active-publisher).
 */
public interface InstagramPublisher {

    /** Stable key used for publisher selection (app.instagram.active-publisher), e.g. "meta-graph-api". */
    String getPublisherName();

    /** Verifies the given credentials actually resolve to a usable Instagram Business Account. */
    InstagramAccountInfo verifyAccount(String businessAccountId, String accessToken);

    InstagramPublishResult publish(InstagramPublishRequest request);
}
