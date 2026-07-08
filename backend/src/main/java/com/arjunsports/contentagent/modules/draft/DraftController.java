package com.arjunsports.contentagent.modules.draft;

import com.arjunsports.contentagent.common.dto.ApiResponse;
import com.arjunsports.contentagent.common.dto.PageResponse;
import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.draft.dto.CreateDraftRequest;
import com.arjunsports.contentagent.modules.draft.dto.DraftResponse;
import com.arjunsports.contentagent.modules.draft.dto.UpdateDraftRequest;
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
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DraftController {

    private final DraftService draftService;
    private final DraftValidator draftValidator;

    @PostMapping
    public ApiResponse<DraftResponse> create(@Valid @RequestBody CreateDraftRequest request) {
        return ApiResponse.success("Draft saved", draftService.createDraft(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DraftResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateDraftRequest request) {
        return ApiResponse.success("Draft updated", draftService.updateDraft(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        draftService.deleteDraft(id);
        return ApiResponse.success("Draft deleted", null);
    }

    @GetMapping
    public ApiResponse<PageResponse<DraftResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) UUID mediaId,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable) {
        DraftStatus parsedStatus = draftValidator.parseStatus(status);
        return ApiResponse.success(PageResponse.from(
                draftService.listDrafts(search, parsedStatus, contentType, mediaId, includeDeleted, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<DraftResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(draftService.getDraft(id));
    }

    @PostMapping("/{id}/duplicate")
    public ApiResponse<DraftResponse> duplicate(@PathVariable UUID id) {
        return ApiResponse.success("Draft duplicated", draftService.duplicateDraft(id));
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<DraftResponse> restore(@PathVariable UUID id) {
        return ApiResponse.success("Draft restored", draftService.restoreDraft(id));
    }

    @PostMapping("/{id}/finalize")
    public ApiResponse<DraftResponse> finalize(@PathVariable UUID id) {
        return ApiResponse.success("Draft finalized", draftService.finalizeDraft(id));
    }
}
