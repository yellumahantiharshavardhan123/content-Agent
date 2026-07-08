package com.arjunsports.contentagent.modules.ai.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AIProviderProperties {

    /** Which AIProvider bean (by getProviderName()) is used. Future values: gemini, claude, azure-openai, ollama. */
    private String activeProvider = "openai";

    private Provider provider = new Provider();

    private Generation generation = new Generation();

    @Getter
    @Setter
    public static class Provider {
        private String baseUrl;
        private String apiKey;
        private String model;
    }

    @Getter
    @Setter
    public static class Generation {
        private int timeoutSeconds = 30;
        private int maxTokens = 700;
        private double temperature = 0.8;
    }
}
