package com.arjunsports.contentagent.modules.instagram.entity;

/** Every action recorded in the {@link InstagramPublishHistory} append-only log. */
public enum InstagramHistoryAction {
    CONNECT,
    DISCONNECT,
    PUBLISH_ATTEMPT,
    PUBLISH_SUCCESS,
    PUBLISH_FAILURE,
    RETRY
}
