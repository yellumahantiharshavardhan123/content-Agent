package com.arjunsports.contentagent.modules.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromptRepository extends JpaRepository<PromptTemplate, UUID> {

    Optional<PromptTemplate> findByContentTypeAndActiveTrue(ContentType contentType);

    List<PromptTemplate> findByContentTypeOrderByVersionDesc(ContentType contentType);

    List<PromptTemplate> findByActiveTrue();
}
