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

/**
 * A versioned, admin-editable prompt for one content type. Never mutated in
 * place: {@code PromptService.update(...)} deactivates the current row and
 * inserts a new one with {@code version + 1}, so prompt history is never
 * lost and {@code generated_content.prompt_template_id} always points at
 * the exact version that produced a given piece of content.
 */
@Getter
@Setter
@Entity
@Table(name = "prompt_templates")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptTemplate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 40)
    private ContentType contentType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    /** Persona/instructions sent as the system message. */
    @Column(name = "system_prompt", columnDefinition = "TEXT", nullable = false)
    private String systemPrompt;

    /** The user-message template, containing {{placeholder}} tokens resolved by PromptBuilder. */
    @Column(name = "user_prompt_template", columnDefinition = "TEXT", nullable = false)
    private String userPromptTemplate;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
