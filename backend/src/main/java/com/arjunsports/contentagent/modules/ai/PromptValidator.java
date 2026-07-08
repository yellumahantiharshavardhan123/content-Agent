package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class PromptValidator {

    /** Cross-field rule that a plain @NotNull/@NotBlank annotation can't express: at least one input is required. */
    public void validateContext(GenerationContext context) {
        if (!context.hasAnyInput()) {
            throw new BadRequestException(
                    "Provide at least one input: select a media item or fill in notes/event/achievement/"
                            + "competition/training/coach details");
        }
    }

    public void validateTemplate(PromptTemplate template, ContentType contentType) {
        if (template == null) {
            throw new BadRequestException("No active prompt template is configured for " + contentType);
        }
        if (template.getUserPromptTemplate() == null || template.getUserPromptTemplate().isBlank()) {
            throw new BadRequestException("The prompt template for " + contentType + " is empty");
        }
    }
}
