package com.arjunsports.contentagent.storage;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    public static final String PUBLIC_URL_CLIENT = "minioPublicUrlClient";

    private final StorageProperties storageProperties;

    /** Used for every real object operation (upload, delete, bucket checks). */
    @Bean
    @Primary
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(storageProperties.getEndpoint())
                .credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey())
                .region(storageProperties.getRegion())
                .build();
    }

    /**
     * Used only to sign presigned GET URLs against the publicly reachable
     * endpoint, so browsers outside the Docker network can actually load
     * them. Same credentials/region - only the host differs.
     */
    @Bean(PUBLIC_URL_CLIENT)
    public MinioClient minioPublicUrlClient() {
        return MinioClient.builder()
                .endpoint(storageProperties.getPublicEndpoint())
                .credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey())
                .region(storageProperties.getRegion())
                .build();
    }
}
