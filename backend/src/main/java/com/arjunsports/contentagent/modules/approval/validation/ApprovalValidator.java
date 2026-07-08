package com.arjunsports.contentagent.modules.approval.validation;

import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ConflictException;
import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import com.arjunsports.contentagent.modules.draft.ContentDraft;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

@Component
public class ApprovalValidator {

    /** Parses a status query value, or returns {@code null} if none was supplied. Rejects unknown values. */
    public ApprovalStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return ApprovalStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid status: " + rawStatus + ". Must be one of " + Arrays.toString(ApprovalStatus.values()));
        }
    }

    /** Parses an ISO-8601 instant query value, or returns {@code null} if none was supplied. */
    public Instant parseInstant(String rawInstant, String paramName) {
        if (rawInstant == null || rawInstant.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(rawInstant.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException(
                    "Invalid " + paramName + ": " + rawInstant + ". Must be an ISO-8601 instant, e.g. 2026-07-08T00:00:00Z");
        }
    }

    public void validateDraftSubmittable(ContentDraft draft) {
        if (draft.isDeleted()) {
            throw new BadRequestException("Cannot submit a deleted draft for approval");
        }
    }

    public void validateNoPendingApproval(Approval existingPending) {
        if (existingPending != null) {
            throw new ConflictException("This draft already has a pending approval request");
        }
    }

    public void validateNoActiveApproval(Approval existingReadyForPublish) {
        if (existingReadyForPublish != null) {
            throw new ConflictException("This draft has already been approved and is ready for publish");
        }
    }

    public void validatePending(Approval approval, String action) {
        if (approval.getStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new ConflictException(
                    "Only a pending approval can be " + action + " (current status: " + approval.getStatus() + ")");
        }
    }

    public void validateDraftApprovable(ContentDraft draft) {
        if (draft == null || draft.isDeleted()) {
            throw new BadRequestException("Cannot approve content whose draft has been deleted");
        }
    }
}
