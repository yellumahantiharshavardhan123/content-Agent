package com.arjunsports.contentagent.modules.ai.provider;

public class AIProviderException extends RuntimeException {

    public enum Reason {
        TIMEOUT,
        UNAVAILABLE,
        INVALID_RESPONSE,
        RATE_LIMITED,
        UNKNOWN
    }

    private final Reason reason;

    public AIProviderException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public AIProviderException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
