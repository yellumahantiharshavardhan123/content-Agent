package com.arjunsports.contentagent.modules.ai;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Resolves a PromptTemplate's {{placeholder}} tokens against a GenerationContext. */
@Component
public class PromptBuilder {

    private static final String ACADEMY_NAME = "Arjun Sports Shooting Academy";

    public BuiltPrompt build(PromptTemplate template, GenerationContext context) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("academyName", ACADEMY_NAME);
        values.put("mediaFileName", nullToEmpty(context.mediaFileName()));
        values.put("mediaDescription", nullToEmpty(context.mediaDescription()));
        values.put("manualNotes", nullToEmpty(context.manualNotes()));
        values.put("eventDetails", nullToEmpty(context.eventDetails()));
        values.put("achievement", nullToEmpty(context.achievement()));
        values.put("competitionResults", nullToEmpty(context.competitionResults()));
        values.put("trainingSession", nullToEmpty(context.trainingSession()));
        values.put("coachNotes", nullToEmpty(context.coachNotes()));

        String resolvedSystemPrompt = resolve(template.getSystemPrompt(), values);
        String resolvedUserPrompt = resolve(template.getUserPromptTemplate(), values).trim();

        return new BuiltPrompt(resolvedSystemPrompt, resolvedUserPrompt);
    }

    private String resolve(String text, Map<String, String> values) {
        String resolved = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return resolved;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record BuiltPrompt(String systemPrompt, String userPrompt) {
    }
}
