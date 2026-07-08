package com.arjunsports.contentagent.modules.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AIResponseParserTest {

    private final AIResponseParser parser = new AIResponseParser();

    @Test
    void parse_plainText_trimsWhitespace() {
        assertThat(parser.parse("  hello world  \n")).isEqualTo("hello world");
    }

    @Test
    void parse_codeFencedResponse_stripsFence() {
        String raw = "```\nGenerated caption text\n```";
        assertThat(parser.parse(raw)).isEqualTo("Generated caption text");
    }

    @Test
    void parse_codeFencedWithLanguageTag_stripsFence() {
        String raw = "```markdown\nSome caption here\n```";
        assertThat(parser.parse(raw)).isEqualTo("Some caption here");
    }

    @Test
    void parse_wrappedInQuotes_stripsQuotes() {
        assertThat(parser.parse("\"A quoted caption\"")).isEqualTo("A quoted caption");
    }

    @Test
    void parse_nullInput_returnsEmptyString() {
        assertThat(parser.parse(null)).isEmpty();
    }
}
