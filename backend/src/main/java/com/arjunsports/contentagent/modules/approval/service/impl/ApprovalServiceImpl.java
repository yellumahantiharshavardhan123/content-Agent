package com.arjunsports.contentagent.modules.approval.service.impl;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.notification.NotificationType;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.arjunsports.contentagent.modules.approval.dto.AddCommentRequest;
import com.arjunsports.contentagent.modules.approval.dto.ApproveRequest;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalCommentResponse;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalHistoryResponse;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalResponse;
import com.arjunsports.contentagent.modules.approval.dto.RejectRequest;
import com.arjunsports.contentagent.modules.approval.dto.SubmitApprovalRequest;
import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalAction;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalComment;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalHistory;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import com.arjunsports.contentagent.modules.approval.mapper.ApprovalMapper;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalCommentRepository;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalHistoryRepository;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalRepository;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalSpecifications;
import com.arjunsports.contentagent.modules.approval.service.ApprovalService;
import com.arjunsports.contentagent.modules.approval.validation.ApprovalValidator;
import com.arjunsports.contentagent.modules.draft.ContentDraft;
import com.arjunsports.contentagent.modules.draft.DraftRepository;
import com.arjunsports.contentagent.modules.draft.DraftStatus;
import com.arjunsports.contentagent.modules.user.Role;
import com.arjunsports.contentagent.modules.user.User;
import com.arjunsports.contentagent.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final ApprovalCommentRepository approvalCommentRepository;
    private final DraftRepository draftRepository;
    private final UserRepository userRepository;
    private final ApprovalValidator approvalValidator;
    private final ApprovalMapper approvalMapper;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ApprovalResponse submit(SubmitApprovalRequest request) {
        AuthenticatedActor actor = requireActor();

        ContentDraft draft = draftRepository.findById(request.contentId())
                .orElseThrow(() -> ResourceNotFoundException.of("ContentDraft", request.contentId()));
        approvalValidator.validateDraftSubmittable(draft);
        approvalValidator.validateNoPendingApproval(
                approvalRepository.findByContentIdAndStatus(draft.getId(), ApprovalStatus.PENDING_APPROVAL).orElse(null));
        approvalValidator.validateNoActiveApproval(
                approvalRepository.findByContentIdAndStatus(draft.getId(), ApprovalStatus.READY_FOR_PUBLISH).orElse(null));

        Approval approval = Approval.builder()
                .contentId(draft.getId())
                .contentTitle(draft.getTitle())
                .contentType(draft.getContentType())
                .status(ApprovalStatus.PENDING_APPROVAL)
                .build();
        Approval saved = approvalRepository.save(approval);

        draft.setStatus(DraftStatus.READY_FOR_REVIEW);
        draftRepository.save(draft);

        recordHistory(saved, ApprovalAction.SUBMIT, null, ApprovalStatus.PENDING_APPROVAL, actor, null);
        auditLogService.record(ActivityAction.SUBMIT, "Approval", saved.getId(),
                Map.of("contentId", draft.getId().toString()));
        notifyActiveAdmins(NotificationType.APPROVAL_REQUIRED, "Approval requested",
                draft.getTitle() + " was submitted for approval", saved.getId());

        return approvalMapper.toResponse(saved, List.of());
    }

    @Override
    @Transactional
    public ApprovalResponse approve(UUID approvalId, ApproveRequest request) {
        AuthenticatedActor actor = requireActor();
        Approval approval = findOrThrow(approvalId);
        approvalValidator.validatePending(approval, "approved");

        ContentDraft draft = draftRepository.findById(approval.getContentId()).orElse(null);
        approvalValidator.validateDraftApprovable(draft);

        ApprovalStatus previousStatus = approval.getStatus();
        approval.setStatus(ApprovalStatus.READY_FOR_PUBLISH);
        approval.setReviewerId(actor.getId());
        approval.setApprovedAt(Instant.now());
        if (request.remarks() != null && !request.remarks().isBlank()) {
            approval.setRemarks(request.remarks());
        }
        Approval saved = approvalRepository.save(approval);

        draft.setStatus(DraftStatus.APPROVED);
        draftRepository.save(draft);

        recordHistory(saved, ApprovalAction.APPROVE, previousStatus, ApprovalStatus.READY_FOR_PUBLISH, actor, request.remarks());
        auditLogService.record(ActivityAction.APPROVE, "Approval", saved.getId());
        notifyIfPresent(saved.getCreatedBy(), NotificationType.APPROVED, "Content approved",
                (saved.getContentTitle() != null ? saved.getContentTitle() : "Your submission") + " was approved and is ready for publish",
                saved.getId());

        return approvalMapper.toResponse(saved, commentsFor(saved.getId()));
    }

    @Override
    @Transactional
    public ApprovalResponse reject(UUID approvalId, RejectRequest request) {
        AuthenticatedActor actor = requireActor();
        Approval approval = findOrThrow(approvalId);
        approvalValidator.validatePending(approval, "rejected");

        ApprovalStatus previousStatus = approval.getStatus();
        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setReviewerId(actor.getId());
        approval.setRejectedAt(Instant.now());
        approval.setRemarks(request.remarks());
        Approval saved = approvalRepository.save(approval);

        draftRepository.findById(approval.getContentId()).ifPresent(draft -> {
            if (!draft.isDeleted()) {
                draft.setStatus(DraftStatus.DRAFT);
                draftRepository.save(draft);
            }
        });

        recordHistory(saved, ApprovalAction.REJECT, previousStatus, ApprovalStatus.REJECTED, actor, request.remarks());
        auditLogService.record(ActivityAction.REJECT, "Approval", saved.getId());
        notifyIfPresent(saved.getCreatedBy(), NotificationType.REJECTED, "Content rejected",
                (saved.getContentTitle() != null ? saved.getContentTitle() : "Your submission") + " was rejected: " + request.remarks(),
                saved.getId());

        return approvalMapper.toResponse(saved, commentsFor(saved.getId()));
    }

    @Override
    @Transactional
    public ApprovalCommentResponse addComment(UUID approvalId, AddCommentRequest request) {
        AuthenticatedActor actor = requireActor();
        Approval approval = findOrThrow(approvalId);

        ApprovalComment comment = ApprovalComment.builder()
                .approvalId(approval.getId())
                .contentId(approval.getContentId())
                .authorId(actor.getId())
                .authorEmail(actor.getEmail())
                .comment(request.comment())
                .build();
        ApprovalComment saved = approvalCommentRepository.save(comment);

        recordHistory(approval, ApprovalAction.COMMENT, approval.getStatus(), approval.getStatus(), actor, request.comment());
        auditLogService.record(ActivityAction.COMMENT, "Approval", approval.getId());

        UUID notifyTarget = actor.getId().equals(approval.getCreatedBy()) ? approval.getReviewerId() : approval.getCreatedBy();
        notifyIfPresent(notifyTarget, NotificationType.APPROVAL_COMMENT_ADDED, "New review comment",
                "A new comment was added to the review of " + (approval.getContentTitle() != null ? approval.getContentTitle() : "your submission"),
                approval.getId());

        return ApprovalCommentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalResponse> listPending(
            ApprovalStatus status, String search, Instant dateFrom, Instant dateTo, Pageable pageable) {
        ApprovalStatus effectiveStatus = status != null ? status : ApprovalStatus.PENDING_APPROVAL;
        var spec = ApprovalSpecifications.build(effectiveStatus, search, dateFrom, dateTo);
        return approvalRepository.findAll(spec, pageable)
                .map(approval -> approvalMapper.toResponse(approval, List.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalHistoryResponse> getHistory(UUID contentId, Pageable pageable) {
        return approvalHistoryRepository.findByContentIdOrderByCreatedAtDesc(contentId, pageable)
                .map(ApprovalHistoryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalResponse getById(UUID id) {
        Approval approval = findOrThrow(id);
        return approvalMapper.toResponse(approval, commentsFor(id));
    }

    private List<ApprovalComment> commentsFor(UUID approvalId) {
        return approvalCommentRepository.findByApprovalIdOrderByCreatedAtAsc(approvalId);
    }

    private Approval findOrThrow(UUID id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Approval", id));
    }

    private void recordHistory(Approval approval, ApprovalAction action, ApprovalStatus previousStatus,
            ApprovalStatus newStatus, AuthenticatedActor actor, String remarks) {
        ApprovalHistory history = ApprovalHistory.builder()
                .approvalId(approval.getId())
                .contentId(approval.getContentId())
                .action(action)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .remarks(remarks)
                .build();
        approvalHistoryRepository.save(history);
    }

    private void notifyActiveAdmins(NotificationType type, String title, String message, UUID approvalId) {
        List<User> admins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);
        String link = "/approvals/" + approvalId;
        for (User admin : admins) {
            notificationService.notify(admin.getId(), type, title, message, link);
        }
    }

    private void notifyIfPresent(UUID recipientId, NotificationType type, String title, String message, UUID approvalId) {
        if (recipientId == null) {
            return;
        }
        notificationService.notify(recipientId, type, title, message, "/approvals/" + approvalId);
    }

    private AuthenticatedActor requireActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthenticatedActor actor)) {
            throw new UnauthorizedException("No authenticated user in context");
        }
        return actor;
    }
}
