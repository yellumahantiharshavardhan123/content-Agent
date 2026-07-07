package com.arjunsports.contentagent.common.audit;

import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void record(ActivityAction action, String entityType, UUID entityId, Map<String, Object> metadata) {
        AuthenticatedActor actor = currentActor();
        ActivityLog entry = ActivityLog.builder()
                .actorId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(serialize(metadata))
                .build();
        activityLogRepository.save(entry);
    }

    @Override
    @Transactional
    public void record(ActivityAction action, String entityType, UUID entityId) {
        record(action, entityType, entityId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> findByEntity(String entityType, UUID entityId, Pageable pageable) {
        return activityLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> findByActor(UUID actorId, Pageable pageable) {
        return activityLogRepository.findByActorId(actorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> findByAction(ActivityAction action, Pageable pageable) {
        return activityLogRepository.findByAction(action, pageable);
    }

    private AuthenticatedActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getPrincipal() instanceof AuthenticatedActor actor ? actor : null;
    }

    private String serialize(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit metadata for entry, storing without metadata", e);
            return null;
        }
    }
}
