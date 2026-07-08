package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptValidatorTest {

    private final PromptValidator validator = new PromptValidator();

    @Test
    void validateContext_allFieldsEmpty_throwsBadRequest() {
        assertThatThrownBy(() -> validator.validateContext(GenerationContext.builder().build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one input");
    }

    @Test
    void validateContext_onlyMediaIdPresent_isValid() {
        GenerationContext context = GenerationContext.builder().mediaId(UUID.randomUUID()).build();
        assertThatCode(() -> validator.validateContext(context)).doesNotThrowAnyException();
    }

    @Test
    void validateContext_onlyManualNotesPresent_isValid() {
        GenerationContext context = GenerationContext.builder().manualNotes("some notes").build();
        assertThatCode(() -> validator.validateContext(context)).doesNotThrowAnyException();
    }

    @Test
    void validateTemplate_nullTemplate_throwsBadRequest() {
        assertThatThrownBy(() -> validator.validateTemplate(null, ContentType.HASHTAGS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No active prompt template");
    }

    @Test
    void validateTemplate_blankUserPrompt_throwsBadRequest() {
        PromptTemplate template = PromptTemplate.builder().userPromptTemplate("   ").build();
        assertThatThrownBy(() -> validator.validateTemplate(template, ContentType.HASHTAGS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }
}
