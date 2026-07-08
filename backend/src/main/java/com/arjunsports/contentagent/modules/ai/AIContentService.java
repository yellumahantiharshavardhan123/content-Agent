package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.modules.ai.dto.GenerateContentRequest;
import com.arjunsports.contentagent.modules.ai.dto.GeneratedContentResponse;
import com.arjunsports.contentagent.modules.ai.dto.GenerationHistoryResponse;
import com.arjunsports.contentagent.modules.ai.dto.UpdateGeneratedContentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AIContentService {

    GeneratedContentResponse generate(GenerateContentRequest request);

    GeneratedContentResponse regenerate(UUID contentId);

    Page<GenerationHistoryResponse> getHistory(Pageable pageable);

    Page<GeneratedContentResponse> listContent(UUID mediaId, ContentType contentType, Pageable pageable);

    GeneratedContentResponse getContent(UUID id);

    GeneratedContentResponse updateContent(UUID id, UpdateGeneratedContentRequest request);

    void deleteContent(UUID id);
}
