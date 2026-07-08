package com.arjunsports.contentagent.modules.ai;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Guards the AI provider from being hammered: 10 generate/regenerate calls per minute per user. */
@Component
public class GenerationRateLimiter {

    private static final int CAPACITY = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<UUID, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(UUID userId) {
        return buckets.computeIfAbsent(userId, id -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(CAPACITY, WINDOW))
                .build();
    }
}
