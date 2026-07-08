package com.arjunsports.contentagent.modules.media;

import com.arjunsports.contentagent.modules.media.dto.MediaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MediaService {

    MediaResponse upload(MultipartFile file, String description);

    MediaResponse get(UUID id);

    Page<MediaResponse> list(MediaType mediaType, Pageable pageable);

    void delete(UUID id);
}
