package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.common.dto.ApiResponse;
import com.arjunsports.contentagent.modules.ai.dto.PromptTemplateResponse;
import com.arjunsports.contentagent.modules.ai.dto.UpdatePromptRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PromptController {

    private final PromptService promptService;

    @GetMapping
    public ApiResponse<List<PromptTemplateResponse>> list() {
        return ApiResponse.success(promptService.listActive());
    }

    @GetMapping("/{id}")
    public ApiResponse<PromptTemplateResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(promptService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromptTemplateResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdatePromptRequest request) {
        return ApiResponse.success("Prompt updated", promptService.update(id, request));
    }
}
