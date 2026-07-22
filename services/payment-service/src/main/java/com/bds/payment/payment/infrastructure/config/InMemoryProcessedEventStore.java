package com.bds.payment.payment.infrastructure.config;

import com.bds.messaging.idempotency.ProcessedEventStore;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Cache<UUID, Boolean> processed = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(10_000).build();

    @Override
    public boolean markProcessed(UUID eventId) {
        return processed.asMap().putIfAbsent(eventId, Boolean.TRUE) == null;
    }
}
