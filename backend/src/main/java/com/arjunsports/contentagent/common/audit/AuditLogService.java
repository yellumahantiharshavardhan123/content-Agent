package com.arjunsports.contentagent.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

/**
 * Write/read gateway to the activity log. Every module's service layer
 * calls {@code record(...)} on create/update/delete/generate/approve/
 * reject/publish/schedule so the audit trail is complete from the first
 * write, regardless of when the dedicated viewer UI is built.
 */
public interface AuditLogService {

    void record(ActivityAction action, String entityType, UUID entityId, Map<String, Object> metadata);

    void record(ActivityAction action, String entityType, UUID entityId);

    Page<ActivityLog> findByEntity(String entityType, UUID entityId, Pageable pageable);

    Page<ActivityLog> findByActor(UUID actorId, Pageable pageable);

    Page<ActivityLog> findByAction(ActivityAction action, Pageable pageable);
}
