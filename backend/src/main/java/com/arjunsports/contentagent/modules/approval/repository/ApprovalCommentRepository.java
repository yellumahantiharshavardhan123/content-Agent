package com.arjunsports.contentagent.modules.approval.repository;

import com.arjunsports.contentagent.modules.approval.entity.ApprovalComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalCommentRepository extends JpaRepository<ApprovalComment, UUID> {

    List<ApprovalComment> findByApprovalIdOrderByCreatedAtAsc(UUID approvalId);
}
