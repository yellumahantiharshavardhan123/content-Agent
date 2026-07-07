package com.arjunsports.contentagent.security;

import com.arjunsports.contentagent.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";
    private static final String BEARER_PREFIX = "Bearer ";

    private final CookieProperties cookieProperties;
    private final JwtTokenProvider jwtTokenProvider;

    public ResponseCookie buildAccessTokenCookie(String token) {
        return baseCookie(ACCESS_TOKEN_COOKIE, token, "/")
                .maxAge(jwtTokenProvider.getAccessTokenTtl())
                .build();
    }

    public ResponseCookie buildRefreshTokenCookie(String token) {
        return baseCookie(REFRESH_TOKEN_COOKIE, token, REFRESH_TOKEN_PATH)
                .maxAge(jwtTokenProvider.getRefreshTokenTtl())
                .build();
    }

    public ResponseCookie expireAccessTokenCookie() {
        return baseCookie(ACCESS_TOKEN_COOKIE, "", "/").maxAge(0).build();
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return baseCookie(REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_PATH).maxAge(0).build();
    }

    public Optional<String> extractAccessToken(HttpServletRequest request) {
        Optional<String> fromCookie = extractCookie(request, ACCESS_TOKEN_COOKIE);
        if (fromCookie.isPresent()) {
            return fromCookie;
        }
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookie(request, REFRESH_TOKEN_COOKIE);
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(path);
        if (StringUtils.hasText(cookieProperties.getDomain())) {
            builder.domain(cookieProperties.getDomain());
        }
        return builder;
    }
}
