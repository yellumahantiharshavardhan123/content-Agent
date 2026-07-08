package com.arjunsports.contentagent.modules.media;

import com.arjunsports.contentagent.common.dto.ApiResponse;
import com.arjunsports.contentagent.common.dto.PageResponse;
import com.arjunsports.contentagent.modules.media.dto.MediaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MediaResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        return ApiResponse.success("File uploaded", mediaService.upload(file, description));
    }

    @GetMapping
    public ApiResponse<PageResponse<MediaResponse>> list(
            @RequestParam(value = "type", required = false) MediaType type, Pageable pageable) {
        return ApiResponse.success(PageResponse.from(mediaService.list(type, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<MediaResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(mediaService.get(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        mediaService.delete(id);
        return ApiResponse.success("Media deleted", null);
    }
}
