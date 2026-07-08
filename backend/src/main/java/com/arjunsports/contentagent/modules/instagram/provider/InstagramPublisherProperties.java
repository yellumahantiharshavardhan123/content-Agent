package com.arjunsports.contentagent.modules.instagram.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.instagram")
public class InstagramPublisherProperties {

    /** Which InstagramPublisher bean (by getPublisherName()) is used. */
    private String activePublisher = "meta-graph-api";

    private String graphApiBaseUrl;
    private String appId;
    private String appSecret;

    /** Optional - if set alongside accessToken and no account is connected yet, auto-connects on startup. */
    private String businessAccountId;
    private String accessToken;
}
