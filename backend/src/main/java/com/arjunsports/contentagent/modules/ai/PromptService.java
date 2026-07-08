package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.modules.ai.dto.PromptTemplateResponse;
import com.arjunsports.contentagent.modules.ai.dto.UpdatePromptRequest;

import java.util.List;
import java.util.UUID;

public interface PromptService {

    List<PromptTemplateResponse> listActive();

    PromptTemplateResponse get(UUID id);

    /** Deactivates the current version and inserts a new one - prompt history is never overwritten. */
    PromptTemplateResponse update(UUID id, UpdatePromptRequest request);
}
