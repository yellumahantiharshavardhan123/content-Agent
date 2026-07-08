package com.arjunsports.contentagent.modules.draft;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface DraftRepository extends JpaRepository<ContentDraft, UUID>, JpaSpecificationExecutor<ContentDraft> {

    boolean existsByGeneratedContentIdAndStatusAndDeletedFalseAndIdNot(
            UUID generatedContentId, DraftStatus status, UUID excludedId);
}
