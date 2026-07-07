package com.arjunsports.contentagent.common.security;

import java.util.UUID;

/**
 * Marker interface implemented by the Authentication principal so that
 * cross-cutting infrastructure (JPA auditing, audit log, notifications)
 * can resolve "who is acting" without depending on the concrete user
 * module. The Authentication module's principal type implements this.
 */
public interface AuthenticatedActor {

    UUID getId();

    String getEmail();
}
