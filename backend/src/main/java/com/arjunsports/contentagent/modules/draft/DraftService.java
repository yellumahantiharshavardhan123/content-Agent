package com.arjunsports.contentagent.modules.draft;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.draft.dto.CreateDraftRequest;
import com.arjunsports.contentagent.modules.draft.dto.DraftResponse;
import com.arjunsports.contentagent.modules.draft.dto.UpdateDraftRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DraftService {

    DraftResponse createDraft(CreateDraftRequest request);

    DraftResponse updateDraft(UUID id, UpdateDraftRequest request);

    void deleteDraft(UUID id);

    Page<DraftResponse> listDrafts(
            String search, DraftStatus status, ContentType contentType, UUID mediaId,
            boolean includeDeleted, Pageable pageable);

    DraftResponse getDraft(UUID id);

    DraftResponse duplicateDraft(UUID id);

    DraftResponse restoreDraft(UUID id);

    DraftResponse finalizeDraft(UUID id);
}
