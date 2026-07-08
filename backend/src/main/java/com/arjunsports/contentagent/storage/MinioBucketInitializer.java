package com.arjunsports.contentagent.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures the configured bucket exists once the application is fully up.
 * Deliberately a separate bean from {@link MinioConfig}: resolving the
 * already-built {@link MinioClient} bean from inside a {@code @PostConstruct}
 * on the same class that declares it trips Spring's "bean currently in
 * creation" guard, so bucket creation would silently never run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucketExists() {
        String bucket = storageProperties.getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created storage bucket '{}'", bucket);
            }
        } catch (Exception e) {
            log.warn("Could not verify/create storage bucket '{}' at startup: {}. "
                    + "Uploads will fail until the bucket exists.", bucket, e.getMessage());
        }
    }
}
