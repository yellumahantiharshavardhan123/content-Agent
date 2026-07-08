package com.arjunsports.contentagent.modules.instagram.repository;

import com.arjunsports.contentagent.modules.instagram.entity.InstagramPost;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InstagramPostRepository extends JpaRepository<InstagramPost, UUID> {

    boolean existsByApprovalIdAndStatus(UUID approvalId, InstagramPostStatus status);
}
