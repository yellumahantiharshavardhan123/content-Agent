package com.arjunsports.contentagent.modules.instagram.repository;

import com.arjunsports.contentagent.modules.instagram.entity.InstagramPublishHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InstagramPublishHistoryRepository extends JpaRepository<InstagramPublishHistory, UUID> {

    Page<InstagramPublishHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
