package com.arjunsports.contentagent.modules.instagram.provider;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Selects the active {@link InstagramPublisher} by name (app.instagram.active-publisher).
 * Adding a new publisher (e.g. a future Facebook/TikTok cross-poster) means
 * adding one more {@code @Component implements InstagramPublisher} bean -
 * nothing here or in any caller needs to change.
 */
@Component
@RequiredArgsConstructor
public class PublisherResolver {

    private static final String MOCK_PUBLISHER_NAME = "mock";

    private final List<InstagramPublisher> publishers;
    private final InstagramPublisherProperties properties;

    private Map<String, InstagramPublisher> publishersByName;

    @PostConstruct
    void index() {
        publishersByName = publishers.stream()
                .collect(Collectors.toMap(InstagramPublisher::getPublisherName, p -> p));
    }

    public InstagramPublisher resolve() {
        InstagramPublisher publisher = publishersByName.get(properties.getActivePublisher());
        if (publisher == null) {
            throw new InstagramPublisherException(InstagramPublisherException.Reason.UNAVAILABLE,
                    "No Instagram publisher registered for '" + properties.getActivePublisher() + "'");
        }
        return publisher;
    }

    /** Used by the explicit "always mock, regardless of configuration" endpoint. */
    public InstagramPublisher resolveMock() {
        InstagramPublisher publisher = publishersByName.get(MOCK_PUBLISHER_NAME);
        if (publisher == null) {
            throw new InstagramPublisherException(InstagramPublisherException.Reason.UNAVAILABLE,
                    "Mock publisher is not available outside the dev profile");
        }
        return publisher;
    }
}
