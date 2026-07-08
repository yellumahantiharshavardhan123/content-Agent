package com.arjunsports.contentagent.modules.ai;

import org.springframework.stereotype.Component;

/** Cleans up raw AI provider output before it is persisted/shown to the user. */
@Component
public class AIResponseParser {

    public String parse(String rawContent) {
        if (rawContent == null) {
            return "";
        }
        String cleaned = rawContent.trim();
        cleaned = stripCodeFence(cleaned);
        cleaned = stripWrappingQuotes(cleaned);
        return cleaned.trim();
    }

    private String stripCodeFence(String text) {
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    private String stripWrappingQuotes(String text) {
        if (text.length() > 1 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
