package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.RateLimitExceededException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.arjunsports.contentagent.modules.ai.dto.GenerateContentRequest;
import com.arjunsports.contentagent.modules.ai.dto.GeneratedContentResponse;
import com.arjunsports.contentagent.modules.ai.provider.AIProviderException;
import com.arjunsports.contentagent.modules.media.MediaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIContentServiceImplTest {

    @Mock
    private PromptRepository promptRepository;
    @Mock
    private GeneratedContentRepository generatedContentRepository;
    @Mock
    private GenerationHistoryRepository generationHistoryRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ContentGenerator contentGenerator;
    @Mock
    private GenerationFailureRecorder failureRecorder;

    private final PromptValidator promptValidator = new PromptValidator();
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final GenerationRateLimiter permissiveRateLimiter = new GenerationRateLimiter();

    private AIContentServiceImpl service;
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AIContentServiceImpl(
                promptRepository, generatedContentRepository, generationHistoryRepository, mediaRepository,
                promptValidator, promptBuilder, contentGenerator, permissiveRateLimiter,
                auditLogService, notificationService, failureRecorder);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new TestActor(USER_ID), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generate_noContextProvided_throwsBadRequestBeforeCallingProvider() {
        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.generate(request)).isInstanceOf(BadRequestException.class);

        verify(contentGenerator, never()).generate(anyString(), anyString());
    }

    @Test
    void generate_noActiveTemplate_throwsBadRequest() {
        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, "some notes", null, null, null, null, null);
        when(promptRepository.findByContentTypeAndActiveTrue(ContentType.HASHTAGS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void generate_success_savesContentAndHistoryAndNotifies() {
        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, "training day", null, null, null, null, null);
        PromptTemplate template = PromptTemplate.builder()
                .contentType(ContentType.HASHTAGS)
                .systemPrompt("sys")
                .userPromptTemplate("Notes: {{manualNotes}}")
                .active(true)
                .build();
        template.setId(UUID.randomUUID());
        when(promptRepository.findByContentTypeAndActiveTrue(ContentType.HASHTAGS)).thenReturn(Optional.of(template));
        when(contentGenerator.generate(anyString(), anyString()))
                .thenReturn(new ContentGenerator.GenerationOutcome("#shooting #sports", "gpt-4o-mini", 250));
        when(generatedContentRepository.save(any(GeneratedContent.class))).thenAnswer(inv -> {
            GeneratedContent c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        GeneratedContentResponse response = service.generate(request);

        assertThat(response.status()).isEqualTo(GenerationStatus.SUCCESS);
        assertThat(response.generatedText()).isEqualTo("#shooting #sports");
        verify(generationHistoryRepository).save(any(GenerationHistory.class));
        verify(auditLogService).record(eq(com.arjunsports.contentagent.common.audit.ActivityAction.GENERATE),
                eq("GeneratedContent"), any(), any());
        verify(notificationService).notify(eq(USER_ID), eq(com.arjunsports.contentagent.common.notification.NotificationType.GENERATION_COMPLETED),
                anyString(), anyString(), any());
    }

    @Test
    void generate_providerFailure_recordsFailedHistoryAndRethrows() {
        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, "training day", null, null, null, null, null);
        PromptTemplate template = PromptTemplate.builder()
                .contentType(ContentType.HASHTAGS)
                .systemPrompt("sys").userPromptTemplate("Notes: {{manualNotes}}").active(true).build();
        template.setId(UUID.randomUUID());
        when(promptRepository.findByContentTypeAndActiveTrue(ContentType.HASHTAGS)).thenReturn(Optional.of(template));
        when(contentGenerator.generate(anyString(), anyString()))
                .thenThrow(new AIProviderException(AIProviderException.Reason.TIMEOUT, "timed out"));

        assertThatThrownBy(() -> service.generate(request)).isInstanceOf(AIProviderException.class);

        verify(generatedContentRepository, never()).save(any());
        verify(failureRecorder).recordFailure(isNull(), isNull(), eq(ContentType.HASHTAGS), eq(template.getId()),
                eq(GenerationAction.GENERATE), eq("timed out"), any());
    }

    @Test
    void generate_rateLimited_throwsWithoutCallingProvider() {
        GenerationRateLimiter exhausted = new GenerationRateLimiter();
        for (int i = 0; i < 10; i++) {
            exhausted.tryConsume(USER_ID);
        }
        AIContentServiceImpl limitedService = new AIContentServiceImpl(
                promptRepository, generatedContentRepository, generationHistoryRepository, mediaRepository,
                promptValidator, promptBuilder, contentGenerator, exhausted, auditLogService, notificationService,
                failureRecorder);

        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, "notes", null, null, null, null, null);

        assertThatThrownBy(() -> limitedService.generate(request)).isInstanceOf(RateLimitExceededException.class);
        verify(contentGenerator, never()).generate(anyString(), anyString());
    }

    @Test
    void regenerate_unknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(generatedContentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.regenerate(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private record TestActor(UUID id) implements AuthenticatedActor {
        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public String getEmail() {
            return "actor@test.com";
        }
    }
}
