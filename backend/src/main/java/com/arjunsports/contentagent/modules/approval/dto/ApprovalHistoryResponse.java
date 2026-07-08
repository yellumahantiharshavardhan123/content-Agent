package com.arjunsports.contentagent.modules.approval.dto;

import com.arjunsports.contentagent.modules.approval.entity.ApprovalAction;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalHistory;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ApprovalHistoryResponse(
        UUID id,
        UUID approvalId,
        UUID contentId,
        ApprovalAction action,
        ApprovalStatus previousStatus,
        ApprovalStatus newStatus,
        UUID actorId,
        String actorEmail,
        String remarks,
        Instant createdAt) {

    public static ApprovalHistoryResponse from(ApprovalHistory history) {
        return ApprovalHistoryResponse.builder()
                .id(history.getId())
                .approvalId(history.getApprovalId())
                .contentId(history.getContentId())
                .action(history.getAction())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .actorId(history.getActorId())
                .actorEmail(history.getActorEmail())
                .remarks(history.getRemarks())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
