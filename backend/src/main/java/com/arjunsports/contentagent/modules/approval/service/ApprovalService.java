package com.arjunsports.contentagent.modules.approval.service;

import com.arjunsports.contentagent.modules.approval.dto.AddCommentRequest;
import com.arjunsports.contentagent.modules.approval.dto.ApproveRequest;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalCommentResponse;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalHistoryResponse;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalResponse;
import com.arjunsports.contentagent.modules.approval.dto.RejectRequest;
import com.arjunsports.contentagent.modules.approval.dto.SubmitApprovalRequest;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface ApprovalService {

    ApprovalResponse submit(SubmitApprovalRequest request);

    ApprovalResponse approve(UUID approvalId, ApproveRequest request);

    ApprovalResponse reject(UUID approvalId, RejectRequest request);

    ApprovalCommentResponse addComment(UUID approvalId, AddCommentRequest request);

    Page<ApprovalResponse> listPending(
            ApprovalStatus status, String search, Instant dateFrom, Instant dateTo, Pageable pageable);

    Page<ApprovalHistoryResponse> getHistory(UUID contentId, Pageable pageable);

    ApprovalResponse getById(UUID id);
}
