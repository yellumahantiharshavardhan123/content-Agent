package com.arjunsports.contentagent.modules.ai.provider;

import lombok.Builder;

@Builder
public record AIGenerationResult(String rawContent, String modelUsed) {
}
