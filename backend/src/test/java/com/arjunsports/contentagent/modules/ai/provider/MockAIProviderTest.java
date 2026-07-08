package com.arjunsports.contentagent.modules.ai.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockAIProviderTest {

    private final MockAIProvider provider = new MockAIProvider();

    @Test
    void getProviderName_returnsMock() {
        assertThat(provider.getProviderName()).isEqualTo("mock");
    }

    @Test
    void generate_hashtagsPrompt_returnsHashtagStyleContent() {
        AIGenerationRequest request = AIGenerationRequest.builder()
                .systemPrompt("You are a social media strategist for {{academyName}}, a competitive shooting "
                        + "sports academy. Produce 10 to 15 relevant hashtags mixing branded, niche shooting-sport, "
                        + "and broader reach tags.")
                .userPrompt("Generate hashtags using this context:\n\nAdditional notes: Regional win")
                .build();

        AIGenerationResult result = provider.generate(request);

        assertThat(result.rawContent()).startsWith("#");
        assertThat(result.modelUsed()).isEqualTo("mock-dev-provider");
    }

    @Test
    void generate_reelScriptPrompt_returnsSceneBySceneScript() {
        AIGenerationRequest request = AIGenerationRequest.builder()
                .systemPrompt("You are a short-form video scriptwriter for {{academyName}}'s Instagram/Facebook "
                        + "Reels. Write a scene-by-scene script for a 15-30 second Reel.")
                .userPrompt("Generate reel script using this context:\n\nAdditional notes: New training montage")
                .build();

        AIGenerationResult result = provider.generate(request);

        assertThat(result.rawContent()).contains("Scene 1").contains("Scene 6");
    }

    @Test
    void generate_blogPrompt_returnsMultiParagraphArticle() {
        AIGenerationRequest request = AIGenerationRequest.builder()
                .systemPrompt("You are a content writer producing a website blog article for {{academyName}}. "
                        + "Write a well-structured 400-600 word article.")
                .userPrompt("Generate website blog using this context:\n\nAchievement: Regional gold")
                .build();

        AIGenerationResult result = provider.generate(request);

        assertThat(result.rawContent().split("\n\n").length).isGreaterThanOrEqualTo(3);
    }

    @Test
    void generate_weavesInProvidedContext() {
        AIGenerationRequest request = AIGenerationRequest.builder()
                .systemPrompt("You are a marketing copywriter for {{academyName}} announcing a student or team "
                        + "achievement.")
                .userPrompt("Generate achievement announcement using this context:\n\nAchievement: State Champion 2026")
                .build();

        AIGenerationResult result = provider.generate(request);

        assertThat(result.rawContent()).contains("State Champion 2026");
    }

    @Test
    void generate_unrecognizedPrompt_returnsGenericFallbackNotBlank() {
        AIGenerationRequest request = AIGenerationRequest.builder()
                .systemPrompt("A completely rewritten custom system prompt with no known phrases.")
                .userPrompt("Some free-form user prompt.")
                .build();

        AIGenerationResult result = provider.generate(request);

        assertThat(result.rawContent()).isNotBlank();
    }

    @Test
    void generate_failureTriggerPresent_throwsAIProviderException() {
        AIGenerationRequest request = AIGenerationRequest.builder()
                .systemPrompt("You are a social media strategist for {{academyName}}.")
                .userPrompt("Generate hashtags using this context:\n\nAdditional notes: SIMULATE_FAILURE please")
                .build();

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(AIProviderException.class)
                .satisfies(ex -> assertThat(((AIProviderException) ex).getReason())
                        .isEqualTo(AIProviderException.Reason.UNAVAILABLE));
    }
}
