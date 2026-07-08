package com.arjunsports.contentagent.modules.approval.repository;

import com.arjunsports.contentagent.modules.approval.entity.ApprovalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {

    Page<ApprovalHistory> findByContentIdOrderByCreatedAtDesc(UUID contentId, Pageable pageable);
}
