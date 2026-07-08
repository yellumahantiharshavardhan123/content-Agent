package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.modules.ai.dto.PromptTemplateResponse;
import com.arjunsports.contentagent.modules.ai.dto.UpdatePromptRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    private final PromptRepository promptRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<PromptTemplateResponse> listActive() {
        return promptRepository.findByActiveTrue().stream()
                .map(PromptTemplateResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromptTemplateResponse get(UUID id) {
        return PromptTemplateResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional
    public PromptTemplateResponse update(UUID id, UpdatePromptRequest request) {
        PromptTemplate current = findOrThrow(id);

        current.setActive(false);
        promptRepository.save(current);

        PromptTemplate next = PromptTemplate.builder()
                .contentType(current.getContentType())
                .name(request.name())
                .description(request.description())
                .systemPrompt(request.systemPrompt())
                .userPromptTemplate(request.userPromptTemplate())
                .version(current.getVersion() + 1)
                .active(true)
                .build();

        PromptTemplate saved = promptRepository.save(next);
        auditLogService.record(ActivityAction.SETTINGS_CHANGE, "PromptTemplate", saved.getId(),
                Map.of("contentType", saved.getContentType().name(), "version", String.valueOf(saved.getVersion())));

        return PromptTemplateResponse.from(saved);
    }

    private PromptTemplate findOrThrow(UUID id) {
        return promptRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("PromptTemplate", id));
    }
}
