package com.arjunsports.contentagent.modules.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, UUID> {

    Page<GeneratedContent> findByMediaId(UUID mediaId, Pageable pageable);

    Page<GeneratedContent> findByContentType(ContentType contentType, Pageable pageable);

    Page<GeneratedContent> findByMediaIdAndContentType(UUID mediaId, ContentType contentType, Pageable pageable);
}
