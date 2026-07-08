package com.arjunsports.contentagent.modules.ai.provider;

import lombok.Builder;

@Builder
public record AIGenerationRequest(
        String systemPrompt,
        String userPrompt,
        String model,
        Integer maxTokens,
        Double temperature) {
}
