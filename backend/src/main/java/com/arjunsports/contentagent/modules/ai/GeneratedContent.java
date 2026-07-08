package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * The current, editable output of a generation request - the "library" a
 * user browses, edits, saves as a draft, previews, or deletes. Regenerating
 * overwrites this row's text/status in place; every attempt (success or
 * failure) is additionally appended to {@link GenerationHistory}.
 */
@Getter
@Setter
@Entity
@Table(name = "generated_content")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedContent extends BaseEntity {

    @Column(name = "media_id")
    private UUID mediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 40)
    private ContentType contentType;

    @Column(name = "prompt_template_id")
    private UUID promptTemplateId;

    /** The fully-resolved system + user prompt actually sent to the AI provider. */
    @Column(name = "prompt_used", columnDefinition = "TEXT", nullable = false)
    private String promptUsed;

    @Column(name = "generated_text", columnDefinition = "TEXT")
    private String generatedText;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GenerationStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "is_draft", nullable = false)
    @Builder.Default
    private boolean draft = true;

    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private boolean edited = false;
}
