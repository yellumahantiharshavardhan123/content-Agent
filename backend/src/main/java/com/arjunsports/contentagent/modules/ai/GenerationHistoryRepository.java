package com.arjunsports.contentagent.modules.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GenerationHistoryRepository extends JpaRepository<GenerationHistory, UUID> {

    Page<GenerationHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<GenerationHistory> findByGeneratedContentIdOrderByCreatedAtDesc(UUID generatedContentId, Pageable pageable);
}
