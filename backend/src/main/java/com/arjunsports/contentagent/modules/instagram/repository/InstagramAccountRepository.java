package com.arjunsports.contentagent.modules.instagram.repository;

import com.arjunsports.contentagent.modules.instagram.entity.InstagramAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstagramAccountRepository extends JpaRepository<InstagramAccount, UUID> {

    Optional<InstagramAccount> findByActiveTrue();
}
