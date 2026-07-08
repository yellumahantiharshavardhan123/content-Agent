package com.arjunsports.contentagent.modules.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void build_replacesAllKnownPlaceholders() {
        PromptTemplate template = PromptTemplate.builder()
                .systemPrompt("You write for {{academyName}}.")
                .userPromptTemplate(
                        "Academy: {{academyName}}, file: {{mediaFileName}}, desc: {{mediaDescription}}, "
                                + "notes: {{manualNotes}}, event: {{eventDetails}}, achievement: {{achievement}}, "
                                + "results: {{competitionResults}}, training: {{trainingSession}}, coach: {{coachNotes}}")
                .build();

        GenerationContext context = GenerationContext.builder()
                .mediaFileName("trophy.jpg")
                .mediaDescription("Regional trophy win")
                .manualNotes("great effort")
                .eventDetails("Regional Championship")
                .achievement("1st place")
                .competitionResults("Score 580/600")
                .trainingSession("Morning drills")
                .coachNotes("Excellent focus")
                .build();

        PromptBuilder.BuiltPrompt result = promptBuilder.build(template, context);

        assertThat(result.systemPrompt()).isEqualTo("You write for Arjun Sports Shooting Academy.");
        assertThat(result.userPrompt())
                .contains("Academy: Arjun Sports Shooting Academy")
                .contains("file: trophy.jpg")
                .contains("desc: Regional trophy win")
                .contains("notes: great effort")
                .contains("event: Regional Championship")
                .contains("achievement: 1st place")
                .contains("results: Score 580/600")
                .contains("training: Morning drills")
                .contains("coach: Excellent focus");
    }

    @Test
    void build_missingContextFields_replacedWithEmptyString() {
        PromptTemplate template = PromptTemplate.builder()
                .systemPrompt("sys")
                .userPromptTemplate("Notes: [{{manualNotes}}]")
                .build();

        PromptBuilder.BuiltPrompt result = promptBuilder.build(template, GenerationContext.builder().build());

        assertThat(result.userPrompt()).isEqualTo("Notes: []");
    }
}
