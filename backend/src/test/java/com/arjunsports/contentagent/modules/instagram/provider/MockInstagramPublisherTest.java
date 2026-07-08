package com.arjunsports.contentagent.modules.instagram.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockInstagramPublisherTest {

    private final MockInstagramPublisher publisher = new MockInstagramPublisher();

    @Test
    void getPublisherName_returnsMock() {
        assertThat(publisher.getPublisherName()).isEqualTo("mock");
    }

    @Test
    void verifyAccount_returnsFakeButPlausibleAccountInfo() {
        InstagramAccountInfo info = publisher.verifyAccount("17841400000000000", "any-token");

        assertThat(info.businessAccountId()).isEqualTo("17841400000000000");
        assertThat(info.username()).isNotBlank();
    }

    @Test
    void publish_success_returnsMediaIdAndPermalink() {
        InstagramPublishRequest request = InstagramPublishRequest.builder()
                .businessAccountId("17841400000000000")
                .accessToken("any-token")
                .imageUrl("https://example.com/image.jpg")
                .caption("Regional championship recap")
                .build();

        InstagramPublishResult result = publisher.publish(request);

        assertThat(result.instagramMediaId()).startsWith("mock_media_");
        assertThat(result.permalink()).startsWith("https://www.instagram.com/p/");
    }

    @Test
    void publish_failureTriggerPresent_throwsInstagramPublisherException() {
        InstagramPublishRequest request = InstagramPublishRequest.builder()
                .businessAccountId("17841400000000000")
                .accessToken("any-token")
                .imageUrl("https://example.com/image.jpg")
                .caption("Please SIMULATE_FAILURE for this test")
                .build();

        assertThatThrownBy(() -> publisher.publish(request))
                .isInstanceOf(InstagramPublisherException.class)
                .satisfies(ex -> assertThat(((InstagramPublisherException) ex).getReason())
                        .isEqualTo(InstagramPublisherException.Reason.UNAVAILABLE));
    }
}
