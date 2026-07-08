package com.arjunsports.contentagent.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Abstraction over the object storage backend (MinIO today, swappable for
 * real AWS S3 purely via configuration since both speak the S3 API).
 */
public interface StorageService {

    /** Uploads a stream to the given object key and returns the key unchanged (caller already chose it). */
    String upload(String objectKey, InputStream content, long size, String contentType);

    /** Generates a time-limited, unauthenticated download URL for an object. */
    String presignedGetUrl(String objectKey, Duration expiry);

    void delete(String objectKey);
}
