package com.arjunsports.contentagent.modules.ai.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Talks to any OpenAI-compatible chat-completions endpoint (OpenAI itself,
 * Azure OpenAI's compatible mode, self-hosted vLLM/Ollama servers, etc).
 * All wire-format detail (request/response shape, auth header, timeouts,
 * error mapping) lives entirely in this class - nothing outside the
 * {@code provider} package knows this is HTTP or what the JSON looks like.
 */
@Slf4j
@Component
public class OpenAICompatibleProvider implements AIProvider {

    private static final String PROVIDER_NAME = "openai";

    private final AIProviderProperties properties;
    private RestClient restClient;

    public OpenAICompatibleProvider(AIProviderProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = properties.getGeneration().getTimeoutSeconds() * 1000;
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);

        this.restClient = RestClient.builder()
                .baseUrl(properties.getProvider().getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getProvider().getApiKey())
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public AIGenerationResult generate(AIGenerationRequest request) {
        String model = request.model() != null ? request.model() : properties.getProvider().getModel();

        ChatCompletionRequest body = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatMessage("system", request.systemPrompt()),
                        new ChatMessage("user", request.userPrompt())),
                request.maxTokens() != null ? request.maxTokens() : properties.getGeneration().getMaxTokens(),
                request.temperature() != null ? request.temperature() : properties.getGeneration().getTemperature());

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AIProviderException(AIProviderException.Reason.INVALID_RESPONSE,
                        "AI provider returned no choices");
            }

            String content = response.choices().get(0).message().content();
            if (content == null || content.isBlank()) {
                throw new AIProviderException(AIProviderException.Reason.INVALID_RESPONSE,
                        "AI provider returned empty content");
            }

            return AIGenerationResult.builder()
                    .rawContent(content)
                    .modelUsed(response.model() != null ? response.model() : model)
                    .build();

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new AIProviderException(AIProviderException.Reason.RATE_LIMITED,
                    "AI provider rate limit exceeded", e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("AI provider returned an error response: {}", e.getResponseBodyAsString());
            throw new AIProviderException(AIProviderException.Reason.UNAVAILABLE,
                    "AI provider returned an error: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            boolean isTimeout = e.getCause() instanceof java.net.SocketTimeoutException;
            throw new AIProviderException(
                    isTimeout ? AIProviderException.Reason.TIMEOUT : AIProviderException.Reason.UNAVAILABLE,
                    isTimeout ? "AI provider request timed out" : "AI provider is unreachable", e);
        }
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            @JsonProperty("max_tokens") Integer maxTokens,
            Double temperature) {
    }

    private record ChatCompletionResponse(String model, List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }
}
