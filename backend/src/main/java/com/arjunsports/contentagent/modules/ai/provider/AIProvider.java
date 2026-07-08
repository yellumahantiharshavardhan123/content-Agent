package com.arjunsports.contentagent.modules.ai.provider;

/**
 * Abstraction over an AI text-generation backend. Every provider (OpenAI-
 * compatible today; Gemini/Claude/Azure OpenAI/Ollama in the future) is a
 * separate implementation of this interface - callers (ContentGenerator)
 * never branch on provider type, so adding a new provider never touches
 * business logic outside that provider's own class.
 */
public interface AIProvider {

    /** Stable key used for provider selection (app.ai.active-provider), e.g. "openai". */
    String getProviderName();

    AIGenerationResult generate(AIGenerationRequest request);
}
