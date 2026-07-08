package com.arjunsports.contentagent.modules.media;

import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.modules.media.dto.MediaResponse;
import com.arjunsports.contentagent.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private AuditLogService auditLogService;

    private MediaServiceImpl mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaServiceImpl(mediaRepository, storageService, auditLogService);
    }

    @Test
    void upload_validImage_storesAndReturnsResponseWithPresignedUrl() {
        MockMultipartFile file = new MockMultipartFile("file", "trophy.jpg", "image/jpeg", "fake-bytes".getBytes());
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.presignedGetUrl(anyString(), any(Duration.class))).thenReturn("https://minio/presigned");

        MediaResponse response = mediaService.upload(file, "Regional trophy win");

        assertThat(response.mediaType()).isEqualTo(MediaType.IMAGE);
        assertThat(response.fileName()).isEqualTo("trophy.jpg");
        assertThat(response.url()).isEqualTo("https://minio/presigned");
        verify(storageService).upload(anyString(), any(), anyLong(), eq("image/jpeg"));
    }

    @Test
    void upload_unsupportedContentType_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "archive.zip", "application/zip", "data".getBytes());

        assertThatThrownBy(() -> mediaService.upload(file, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported file type");

        verify(storageService, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void upload_fileExceedsMaxSizeForType_throwsBadRequest() {
        byte[] tooLarge = new byte[3 * 1024 * 1024]; // 3MB > 2MB text-note limit
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", tooLarge);

        assertThatThrownBy(() -> mediaService.upload(file, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds the maximum allowed size");
    }

    @Test
    void upload_emptyFile_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> mediaService.upload(file, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void get_unknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(mediaRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.get(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_marksDeletedAndRemovesFromStorage() {
        UUID id = UUID.randomUUID();
        Media media = Media.builder().storageKey("media/x/file.jpg").mediaType(MediaType.IMAGE).build();
        when(mediaRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(media));
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        mediaService.delete(id);

        ArgumentCaptor<Media> captor = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        verify(storageService).delete("media/x/file.jpg");
    }
}
