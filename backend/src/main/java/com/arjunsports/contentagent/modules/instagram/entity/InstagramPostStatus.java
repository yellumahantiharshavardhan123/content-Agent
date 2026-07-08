package com.arjunsports.contentagent.modules.instagram.entity;

/** Lifecycle of a single {@link InstagramPost} publish attempt. */
public enum InstagramPostStatus {
    PENDING,
    PUBLISHING,
    PUBLISHED,
    FAILED
}
