package com.arjunsports.contentagent.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String endpoint;

    /**
     * Endpoint used only when signing presigned URLs handed back to browsers.
     * Inside Docker, {@link #endpoint} is the internal service DNS name
     * (e.g. http://minio:9000) which the backend uses for real object
     * operations, but a browser outside the Compose network can't resolve
     * that host - presigned URLs must be signed against the publicly
     * reachable address instead (e.g. http://localhost:9000). Defaults to
     * {@link #endpoint} for the non-Docker/local case where they're the same.
     */
    private String publicEndpoint;

    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region = "us-east-1";
    private boolean pathStyleAccess = true;

    public String getPublicEndpoint() {
        return (publicEndpoint == null || publicEndpoint.isBlank()) ? endpoint : publicEndpoint;
    }
}
