package com.arjunsports.contentagent.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force guard on the login endpoint: 5 attempts per
 * minute per client IP. In-memory is sufficient for a single-instance
 * deployment; move to a distributed store (Redis) if scaled horizontally.
 */
@Component
public class LoginRateLimiter {

    private static final int CAPACITY = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key) {
        return buckets.computeIfAbsent(key, k -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(CAPACITY, WINDOW))
                .build();
    }
}
