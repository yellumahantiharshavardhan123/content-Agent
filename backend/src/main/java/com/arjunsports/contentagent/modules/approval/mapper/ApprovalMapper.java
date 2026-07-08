package com.arjunsports.contentagent.modules.approval.mapper;

import com.arjunsports.contentagent.modules.approval.dto.ApprovalCommentResponse;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalResponse;
import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalComment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApprovalMapper {

    public ApprovalResponse toResponse(Approval approval, List<ApprovalComment> comments) {
        return ApprovalResponse.builder()
                .id(approval.getId())
                .contentId(approval.getContentId())
                .contentTitle(approval.getContentTitle())
                .contentType(approval.getContentType())
                .reviewerId(approval.getReviewerId())
                .status(approval.getStatus())
                .remarks(approval.getRemarks())
                .approvedAt(approval.getApprovedAt())
                .rejectedAt(approval.getRejectedAt())
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .createdBy(approval.getCreatedBy())
                .comments(comments.stream().map(ApprovalCommentResponse::from).toList())
                .build();
    }

    public ApprovalResponse toResponse(Approval approval) {
        return toResponse(approval, List.of());
    }
}
