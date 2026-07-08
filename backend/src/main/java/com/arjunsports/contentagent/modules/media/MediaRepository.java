package com.arjunsports.contentagent.modules.media;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    Page<Media> findByDeletedFalse(Pageable pageable);

    Page<Media> findByDeletedFalseAndMediaType(MediaType mediaType, Pageable pageable);

    Optional<Media> findByIdAndDeletedFalse(UUID id);
}
