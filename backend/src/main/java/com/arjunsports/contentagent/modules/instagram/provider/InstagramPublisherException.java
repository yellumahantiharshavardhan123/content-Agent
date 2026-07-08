package com.arjunsports.contentagent.modules.instagram.provider;

public class InstagramPublisherException extends RuntimeException {

    public enum Reason {
        INVALID_CREDENTIALS,
        INVALID_MEDIA,
        RATE_LIMITED,
        TIMEOUT,
        UNAVAILABLE,
        UNKNOWN
    }

    private final Reason reason;

    public InstagramPublisherException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public InstagramPublisherException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
