package com.arjunsports.contentagent.modules.approval.dto;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record ApprovalResponse(
        UUID id,
        UUID contentId,
        String contentTitle,
        ContentType contentType,
        UUID reviewerId,
        ApprovalStatus status,
        String remarks,
        Instant approvedAt,
        Instant rejectedAt,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        List<ApprovalCommentResponse> comments) {
}
