package com.arjunsports.contentagent.modules.instagram.controller;

import com.arjunsports.contentagent.common.dto.ApiResponse;
import com.arjunsports.contentagent.common.dto.PageResponse;
import com.arjunsports.contentagent.modules.instagram.dto.ConnectAccountRequest;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramAccountResponse;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramHistoryResponse;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramPostResponse;
import com.arjunsports.contentagent.modules.instagram.dto.PublishRequest;
import com.arjunsports.contentagent.modules.instagram.service.InstagramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InstagramController {

    private final InstagramService instagramService;

    @PostMapping("/connect")
    public ApiResponse<InstagramAccountResponse> connect(@Valid @RequestBody ConnectAccountRequest request) {
        return ApiResponse.success("Instagram account connected", instagramService.connect(request));
    }

    @GetMapping("/status")
    public ApiResponse<InstagramAccountResponse> status() {
        return ApiResponse.success(instagramService.getStatus());
    }

    @PostMapping("/publish")
    public ApiResponse<InstagramPostResponse> publish(@Valid @RequestBody PublishRequest request) {
        return ApiResponse.success("Published to Instagram", instagramService.publish(request));
    }

    @PostMapping("/publish/mock")
    public ApiResponse<InstagramPostResponse> publishMock(@Valid @RequestBody PublishRequest request) {
        return ApiResponse.success("Published to Instagram (mock)", instagramService.publishMock(request));
    }

    @GetMapping("/history")
    public ApiResponse<PageResponse<InstagramHistoryResponse>> history(Pageable pageable) {
        return ApiResponse.success(PageResponse.from(instagramService.getHistory(pageable)));
    }

    @DeleteMapping("/disconnect")
    public ApiResponse<Void> disconnect() {
        instagramService.disconnect();
        return ApiResponse.success("Instagram account disconnected", null);
    }
}
