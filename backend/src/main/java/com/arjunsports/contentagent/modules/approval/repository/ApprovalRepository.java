package com.arjunsports.contentagent.modules.approval.repository;

import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID>, JpaSpecificationExecutor<Approval> {

    Optional<Approval> findByContentIdAndStatus(UUID contentId, ApprovalStatus status);
}
