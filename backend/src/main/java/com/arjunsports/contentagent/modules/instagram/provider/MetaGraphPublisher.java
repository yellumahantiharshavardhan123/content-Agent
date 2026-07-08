package com.arjunsports.contentagent.modules.instagram.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Talks to the real Meta Graph API's Instagram Content Publishing endpoints:
 * verify an account (GET /{ig-user-id}), create a media container, then
 * publish it - the standard two-step Graph API publish flow. All wire-format
 * detail (query-param auth, response shape, error mapping) lives entirely in
 * this class - nothing outside the {@code provider} package knows this is
 * HTTP or what the JSON looks like.
 */
@Slf4j
@Component
public class MetaGraphPublisher implements InstagramPublisher {

    private static final String PUBLISHER_NAME = "meta-graph-api";

    private final InstagramPublisherProperties properties;
    private final RestClient restClient;

    public MetaGraphPublisher(InstagramPublisherProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.getGraphApiBaseUrl()).build();
    }

    @Override
    public String getPublisherName() {
        return PUBLISHER_NAME;
    }

    @Override
    public InstagramAccountInfo verifyAccount(String businessAccountId, String accessToken) {
        try {
            AccountResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/{id}")
                            .queryParam("fields", "id,username")
                            .queryParam("access_token", accessToken)
                            .build(businessAccountId))
                    .retrieve()
                    .body(AccountResponse.class);

            if (response == null || response.id() == null) {
                throw new InstagramPublisherException(InstagramPublisherException.Reason.UNKNOWN,
                        "Meta Graph API returned no account data");
            }

            return InstagramAccountInfo.builder()
                    .businessAccountId(response.id())
                    .username(response.username())
                    .build();

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new InstagramPublisherException(InstagramPublisherException.Reason.INVALID_CREDENTIALS,
                    "Meta rejected the provided access token", e);
        } catch (HttpClientErrorException e) {
            log.error("Meta Graph API returned an error verifying the account: {}", e.getResponseBodyAsString());
            throw new InstagramPublisherException(InstagramPublisherException.Reason.INVALID_CREDENTIALS,
                    "Meta could not verify this Business Account ID / access token", e);
        } catch (ResourceAccessException e) {
            throw connectivityException(e);
        }
    }

    @Override
    public InstagramPublishResult publish(InstagramPublishRequest request) {
        try {
            MediaContainerResponse container = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/{id}/media")
                            .queryParam("image_url", request.imageUrl())
                            .queryParam("caption", request.caption())
                            .queryParam("access_token", request.accessToken())
                            .build(request.businessAccountId()))
                    .retrieve()
                    .body(MediaContainerResponse.class);

            if (container == null || container.id() == null) {
                throw new InstagramPublisherException(InstagramPublisherException.Reason.UNKNOWN,
                        "Meta Graph API returned no media container id");
            }

            MediaPublishResponse published = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/{id}/media_publish")
                            .queryParam("creation_id", container.id())
                            .queryParam("access_token", request.accessToken())
                            .build(request.businessAccountId()))
                    .retrieve()
                    .body(MediaPublishResponse.class);

            if (published == null || published.id() == null) {
                throw new InstagramPublisherException(InstagramPublisherException.Reason.UNKNOWN,
                        "Meta Graph API returned no published media id");
            }

            String permalink = fetchPermalink(published.id(), request.accessToken());

            return InstagramPublishResult.builder()
                    .instagramMediaId(published.id())
                    .permalink(permalink)
                    .build();

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new InstagramPublisherException(InstagramPublisherException.Reason.RATE_LIMITED,
                    "Meta Graph API rate limit exceeded", e);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new InstagramPublisherException(InstagramPublisherException.Reason.INVALID_CREDENTIALS,
                    "Meta rejected the access token during publish", e);
        } catch (HttpClientErrorException e) {
            log.error("Meta Graph API returned an error publishing: {}", e.getResponseBodyAsString());
            String body = e.getResponseBodyAsString();
            boolean mediaIssue = body != null && body.toLowerCase().contains("media");
            throw new InstagramPublisherException(
                    mediaIssue ? InstagramPublisherException.Reason.INVALID_MEDIA : InstagramPublisherException.Reason.UNAVAILABLE,
                    "Meta Graph API rejected the publish request: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new InstagramPublisherException(InstagramPublisherException.Reason.UNAVAILABLE,
                    "Meta Graph API is currently unavailable", e);
        } catch (ResourceAccessException e) {
            throw connectivityException(e);
        }
    }

    private String fetchPermalink(String mediaId, String accessToken) {
        try {
            PermalinkResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/{id}")
                            .queryParam("fields", "permalink")
                            .queryParam("access_token", accessToken)
                            .build(mediaId))
                    .retrieve()
                    .body(PermalinkResponse.class);
            return response != null ? response.permalink() : null;
        } catch (Exception e) {
            log.warn("Published successfully but could not fetch the permalink for media {}: {}", mediaId, e.getMessage());
            return null;
        }
    }

    private InstagramPublisherException connectivityException(ResourceAccessException e) {
        boolean isTimeout = e.getCause() instanceof java.net.SocketTimeoutException;
        return new InstagramPublisherException(
                isTimeout ? InstagramPublisherException.Reason.TIMEOUT : InstagramPublisherException.Reason.UNAVAILABLE,
                isTimeout ? "Meta Graph API request timed out" : "Meta Graph API is unreachable", e);
    }

    private record AccountResponse(String id, String username) {
    }

    private record MediaContainerResponse(String id) {
    }

    private record MediaPublishResponse(String id) {
    }

    private record PermalinkResponse(String id, String permalink) {
    }
}
