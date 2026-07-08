package com.arjunsports.contentagent.modules.draft;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.ai.GeneratedContent;
import com.arjunsports.contentagent.modules.ai.GeneratedContentRepository;
import com.arjunsports.contentagent.modules.ai.GenerationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused on {@link DraftRepository}/{@link DraftSpecifications} query behavior. Uses the full
 * application context (like every other test in this suite) rather than {@code @DataJpaTest}'s
 * slice, since the slice excludes the security-aware {@code AuditorAware} bean that JPA auditing
 * on {@link com.arjunsports.contentagent.common.entity.BaseEntity} depends on.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DraftRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DraftRepository draftRepository;
    @Autowired
    private GeneratedContentRepository generatedContentRepository;

    @BeforeEach
    void setUp() {
        draftRepository.deleteAll();
        generatedContentRepository.deleteAll();
    }

    @Test
    void findAll_withStatusSpecification_returnsOnlyMatchingStatus() {
        draftRepository.save(draft("Regional recap", "text one", DraftStatus.DRAFT, false));
        draftRepository.save(draft("Training tip", "text two", DraftStatus.APPROVED, false));

        var page = draftRepository.findAll(
                DraftSpecifications.build(null, DraftStatus.APPROVED, null, null, false), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(DraftStatus.APPROVED);
    }

    @Test
    void findAll_excludesDeletedByDefault() {
        ContentDraft deleted = draft("Old draft", "text", DraftStatus.DRAFT, true);
        draftRepository.save(deleted);
        draftRepository.save(draft("Active draft", "text", DraftStatus.DRAFT, false));

        var page = draftRepository.findAll(
                DraftSpecifications.build(null, null, null, null, false), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Active draft");
    }

    @Test
    void findAll_includeDeletedTrue_returnsBoth() {
        draftRepository.save(draft("Old draft", "text", DraftStatus.DRAFT, true));
        draftRepository.save(draft("Active draft", "text", DraftStatus.DRAFT, false));

        var page = draftRepository.findAll(
                DraftSpecifications.build(null, null, null, null, true), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void findAll_searchMatchesTitleOrContentTextCaseInsensitively() {
        draftRepository.save(draft("Regional Championship Recap", "irrelevant", DraftStatus.DRAFT, false));
        draftRepository.save(draft("Weekly Update", "won the regional championship yesterday", DraftStatus.DRAFT, false));
        draftRepository.save(draft("Unrelated", "nothing to see here", DraftStatus.DRAFT, false));

        var page = draftRepository.findAll(
                DraftSpecifications.build("REGIONAL CHAMPIONSHIP", null, null, null, false), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void existsByGeneratedContentIdAndStatusAndDeletedFalseAndIdNot_excludesSameId() {
        GeneratedContent source = GeneratedContent.builder()
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .promptUsed("prompt")
                .generatedText("text")
                .status(GenerationStatus.SUCCESS)
                .build();
        UUID sourceId = generatedContentRepository.save(source).getId();
        ContentDraft approved = draft("Final version", "text", DraftStatus.APPROVED, false);
        approved.setGeneratedContentId(sourceId);
        ContentDraft saved = draftRepository.save(approved);

        boolean existsExcludingSelf = draftRepository.existsByGeneratedContentIdAndStatusAndDeletedFalseAndIdNot(
                sourceId, DraftStatus.APPROVED, saved.getId());
        boolean existsExcludingOther = draftRepository.existsByGeneratedContentIdAndStatusAndDeletedFalseAndIdNot(
                sourceId, DraftStatus.APPROVED, UUID.randomUUID());

        assertThat(existsExcludingSelf).isFalse();
        assertThat(existsExcludingOther).isTrue();
    }

    private ContentDraft draft(String title, String contentText, DraftStatus status, boolean deleted) {
        return ContentDraft.builder()
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .title(title)
                .contentText(contentText)
                .status(status)
                .deleted(deleted)
                .build();
    }
}
