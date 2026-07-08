package com.arjunsports.contentagent.modules.approval.dto;

import com.arjunsports.contentagent.modules.approval.entity.ApprovalComment;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ApprovalCommentResponse(
        UUID id,
        UUID approvalId,
        UUID authorId,
        String authorEmail,
        String comment,
        Instant createdAt) {

    public static ApprovalCommentResponse from(ApprovalComment comment) {
        return ApprovalCommentResponse.builder()
                .id(comment.getId())
                .approvalId(comment.getApprovalId())
                .authorId(comment.getAuthorId())
                .authorEmail(comment.getAuthorEmail())
                .comment(comment.getComment())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
