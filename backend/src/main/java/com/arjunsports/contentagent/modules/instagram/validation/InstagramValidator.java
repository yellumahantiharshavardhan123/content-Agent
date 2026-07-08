package com.arjunsports.contentagent.modules.instagram.validation;

import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ConflictException;
import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import org.springframework.stereotype.Component;

@Component
public class InstagramValidator {

    public void validateNoActiveConnection(boolean alreadyConnected) {
        if (alreadyConnected) {
            throw new ConflictException("An Instagram account is already connected. Disconnect it first.");
        }
    }

    public void validateApprovalPublishable(Approval approval) {
        if (approval.getStatus() != ApprovalStatus.READY_FOR_PUBLISH) {
            throw new BadRequestException(
                    "Only approved, ready-for-publish content can be published to Instagram (current status: "
                            + approval.getStatus() + ")");
        }
    }

    public void validateNotAlreadyPublished(boolean alreadyPublished) {
        if (alreadyPublished) {
            throw new ConflictException("This content has already been published to Instagram");
        }
    }
}
