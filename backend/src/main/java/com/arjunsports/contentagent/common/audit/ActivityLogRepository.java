package com.arjunsports.contentagent.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    Page<ActivityLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    Page<ActivityLog> findByActorId(UUID actorId, Pageable pageable);

    Page<ActivityLog> findByAction(ActivityAction action, Pageable pageable);
}
