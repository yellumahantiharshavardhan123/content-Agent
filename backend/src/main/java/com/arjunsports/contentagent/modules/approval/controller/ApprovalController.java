package com.arjunsports.contentagent.modules.approval.controller;

import com.arjunsports.contentagent.common.dto.ApiResponse;
import com.arjunsports.contentagent.common.dto.PageResponse;
import com.arjunsports.contentagent.modules.approval.dto.AddCommentRequest;
import com.arjunsports.contentagent.modules.approval.dto.ApproveRequest;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalCommentResponse;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalHistoryResponse;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalResponse;
import com.arjunsports.contentagent.modules.approval.dto.RejectRequest;
import com.arjunsports.contentagent.modules.approval.dto.SubmitApprovalRequest;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import com.arjunsports.contentagent.modules.approval.service.ApprovalService;
import com.arjunsports.contentagent.modules.approval.validation.ApprovalValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final ApprovalValidator approvalValidator;

    @PostMapping("/submit")
    public ApiResponse<ApprovalResponse> submit(@Valid @RequestBody SubmitApprovalRequest request) {
        return ApiResponse.success("Submitted for approval", approvalService.submit(request));
    }

    @PostMapping("/approve/{id}")
    public ApiResponse<ApprovalResponse> approve(@PathVariable UUID id, @RequestBody(required = false) ApproveRequest request) {
        return ApiResponse.success("Approved", approvalService.approve(id, request != null ? request : new ApproveRequest(null)));
    }

    @PostMapping("/reject/{id}")
    public ApiResponse<ApprovalResponse> reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest request) {
        return ApiResponse.success("Rejected", approvalService.reject(id, request));
    }

    @PostMapping("/comment/{id}")
    public ApiResponse<ApprovalCommentResponse> comment(@PathVariable UUID id, @Valid @RequestBody AddCommentRequest request) {
        return ApiResponse.success("Comment added", approvalService.addComment(id, request));
    }

    @GetMapping("/pending")
    public ApiResponse<PageResponse<ApprovalResponse>> pending(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Pageable pageable) {
        ApprovalStatus parsedStatus = approvalValidator.parseStatus(status);
        Instant from = approvalValidator.parseInstant(dateFrom, "dateFrom");
        Instant to = approvalValidator.parseInstant(dateTo, "dateTo");
        return ApiResponse.success(PageResponse.from(
                approvalService.listPending(parsedStatus, search, from, to, pageable)));
    }

    @GetMapping("/history/{contentId}")
    public ApiResponse<PageResponse<ApprovalHistoryResponse>> history(@PathVariable UUID contentId, Pageable pageable) {
        return ApiResponse.success(PageResponse.from(approvalService.getHistory(contentId, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApprovalResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(approvalService.getById(id));
    }
}
