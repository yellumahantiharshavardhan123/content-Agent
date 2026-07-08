package com.arjunsports.contentagent.storage;

import com.arjunsports.contentagent.common.exception.BadRequestException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Service
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final MinioClient minioPublicUrlClient;
    private final StorageProperties storageProperties;

    public MinioStorageServiceImpl(
            MinioClient minioClient,
            @Qualifier(MinioConfig.PUBLIC_URL_CLIENT) MinioClient minioPublicUrlClient,
            StorageProperties storageProperties) {
        this.minioClient = minioClient;
        this.minioPublicUrlClient = minioPublicUrlClient;
        this.storageProperties = storageProperties;
    }

    @Override
    public String upload(String objectKey, InputStream content, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(objectKey)
                    .stream(content, size, -1L)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload object '{}' to storage", objectKey, e);
            throw new BadRequestException("Failed to store the uploaded file. Please try again.");
        }
    }

    @Override
    public String presignedGetUrl(String objectKey, Duration expiry) {
        try {
            return minioPublicUrlClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Http.Method.GET)
                    .bucket(storageProperties.getBucket())
                    .object(objectKey)
                    .expiry((int) expiry.toSeconds())
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for object '{}'", objectKey, e);
            throw new BadRequestException("Failed to generate a download link for this file.");
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete object '{}' from storage", objectKey, e);
            throw new BadRequestException("Failed to delete the stored file.");
        }
    }
}
