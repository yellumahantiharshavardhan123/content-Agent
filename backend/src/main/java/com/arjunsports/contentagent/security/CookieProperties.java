package com.arjunsports.contentagent.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

    /** Must be true in any environment served over HTTPS. Defaults false for plain-HTTP local dev. */
    private boolean secure = false;

    private String sameSite = "Lax";

    /** Empty = host-only cookie (recommended for a single-domain deployment). */
    private String domain = "";
}
