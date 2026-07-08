package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.common.dto.ApiResponse;
import com.arjunsports.contentagent.common.dto.PageResponse;
import com.arjunsports.contentagent.modules.ai.dto.GenerateContentRequest;
import com.arjunsports.contentagent.modules.ai.dto.GeneratedContentResponse;
import com.arjunsports.contentagent.modules.ai.dto.GenerationHistoryResponse;
import com.arjunsports.contentagent.modules.ai.dto.RegenerateContentRequest;
import com.arjunsports.contentagent.modules.ai.dto.UpdateGeneratedContentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AIController {

    private final AIContentService aiContentService;

    @PostMapping("/generate")
    public ApiResponse<GeneratedContentResponse> generate(@Valid @RequestBody GenerateContentRequest request) {
        return ApiResponse.success("Content generated", aiContentService.generate(request));
    }

    @PostMapping("/regenerate")
    public ApiResponse<GeneratedContentResponse> regenerate(@Valid @RequestBody RegenerateContentRequest request) {
        return ApiResponse.success("Content regenerated", aiContentService.regenerate(request.contentId()));
    }

    @GetMapping("/history")
    public ApiResponse<PageResponse<GenerationHistoryResponse>> history(Pageable pageable) {
        return ApiResponse.success(PageResponse.from(aiContentService.getHistory(pageable)));
    }

    @GetMapping("/content")
    public ApiResponse<PageResponse<GeneratedContentResponse>> listContent(
            @RequestParam(required = false) UUID mediaId,
            @RequestParam(required = false) ContentType contentType,
            Pageable pageable) {
        return ApiResponse.success(PageResponse.from(aiContentService.listContent(mediaId, contentType, pageable)));
    }

    @GetMapping("/content/{id}")
    public ApiResponse<GeneratedContentResponse> getContent(@PathVariable UUID id) {
        return ApiResponse.success(aiContentService.getContent(id));
    }

    @PutMapping("/content/{id}")
    public ApiResponse<GeneratedContentResponse> updateContent(
            @PathVariable UUID id, @Valid @RequestBody UpdateGeneratedContentRequest request) {
        return ApiResponse.success("Content updated", aiContentService.updateContent(id, request));
    }

    @DeleteMapping("/content/{id}")
    public ApiResponse<Void> deleteContent(@PathVariable UUID id) {
        aiContentService.deleteContent(id);
        return ApiResponse.success("Content deleted", null);
    }
}
