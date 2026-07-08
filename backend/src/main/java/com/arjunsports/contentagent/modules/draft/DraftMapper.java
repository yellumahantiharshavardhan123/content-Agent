package com.arjunsports.contentagent.modules.draft;

import com.arjunsports.contentagent.modules.draft.dto.DraftResponse;
import org.springframework.stereotype.Component;

@Component
public class DraftMapper {

    public DraftResponse toResponse(ContentDraft draft) {
        return DraftResponse.builder()
                .id(draft.getId())
                .generatedContentId(draft.getGeneratedContentId())
                .mediaId(draft.getMediaId())
                .contentType(draft.getContentType())
                .title(draft.getTitle())
                .contentText(draft.getContentText())
                .status(draft.getStatus())
                .deleted(draft.isDeleted())
                .deletedAt(draft.getDeletedAt())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .createdBy(draft.getCreatedBy())
                .updatedBy(draft.getUpdatedBy())
                .build();
    }
}
