package com.arjunsports.contentagent.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Raw secret bytes (UTF-8) used to sign access tokens; must be >= 32 characters (256 bits). */
    private String secret;

    private long accessTokenTtlMinutes = 15;

    private long refreshTokenTtlDays = 7;
}
