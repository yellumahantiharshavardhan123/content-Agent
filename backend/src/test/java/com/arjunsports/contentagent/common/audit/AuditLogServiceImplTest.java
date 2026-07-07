package com.arjunsports.contentagent.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(activityLogRepository, new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    @Test
    void record_persistsEntryWithSerializedMetadataAndNoActorWhenUnauthenticated() {
        UUID entityId = UUID.randomUUID();
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.record(ActivityAction.PUBLISH, "ContentDraft", entityId, Map.of("platform", "instagram"));

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(ActivityAction.PUBLISH);
        assertThat(saved.getEntityType()).isEqualTo("ContentDraft");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getActorId()).isNull();
        assertThat(saved.getMetadata()).contains("instagram");
    }

    @Test
    void record_withoutMetadataOverload_storesNullMetadata() {
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.record(ActivityAction.LOGIN, "User", null);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMetadata()).isNull();
    }
}
