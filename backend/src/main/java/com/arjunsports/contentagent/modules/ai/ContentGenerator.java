package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.modules.ai.provider.AIGenerationRequest;
import com.arjunsports.contentagent.modules.ai.provider.AIGenerationResult;
import com.arjunsports.contentagent.modules.ai.provider.AIProviderResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Orchestrates a single AI call: send the (already-resolved) system/user
 * prompt to whichever provider is active, then clean up its response.
 * Deliberately takes plain prompt strings rather than a PromptTemplate, so
 * regeneration can resend an already-built prompt without going through
 * PromptBuilder again.
 */
@Component
@RequiredArgsConstructor
public class ContentGenerator {

    private final AIProviderResolver providerResolver;
    private final AIResponseParser responseParser;

    public GenerationOutcome generate(String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();

        AIGenerationResult result = providerResolver.resolve().generate(
                AIGenerationRequest.builder()
                        .systemPrompt(systemPrompt)
                        .userPrompt(userPrompt)
                        .build());

        int latencyMs = (int) (System.currentTimeMillis() - start);
        String cleaned = responseParser.parse(result.rawContent());

        return new GenerationOutcome(cleaned, result.modelUsed(), latencyMs);
    }

    public record GenerationOutcome(String generatedText, String aiModel, int latencyMs) {
    }
}
