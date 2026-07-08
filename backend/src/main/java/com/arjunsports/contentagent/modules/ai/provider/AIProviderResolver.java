package com.arjunsports.contentagent.modules.ai.provider;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Selects the active {@link AIProvider} by name (app.ai.active-provider).
 * Adding a new provider (Gemini, Claude, Azure OpenAI, Ollama) means adding
 * one more {@code @Component implements AIProvider} bean - nothing here or
 * in any caller needs to change.
 */
@Component
@RequiredArgsConstructor
public class AIProviderResolver {

    private final List<AIProvider> providers;
    private final AIProviderProperties properties;

    private Map<String, AIProvider> providersByName;

    @PostConstruct
    void index() {
        providersByName = providers.stream()
                .collect(Collectors.toMap(AIProvider::getProviderName, p -> p));
    }

    public AIProvider resolve() {
        AIProvider provider = providersByName.get(properties.getActiveProvider());
        if (provider == null) {
            throw new AIProviderException(AIProviderException.Reason.UNAVAILABLE,
                    "No AI provider registered for '" + properties.getActiveProvider() + "'");
        }
        return provider;
    }
}
