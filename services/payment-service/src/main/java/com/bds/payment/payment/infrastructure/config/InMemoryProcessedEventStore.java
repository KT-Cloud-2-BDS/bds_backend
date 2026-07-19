package com.bds.payment.payment.infrastructure.config;

import com.bds.messaging.idempotency.ProcessedEventStore;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final ConcurrentHashMap<UUID, Boolean> processed = new ConcurrentHashMap<>();

    @Override
    public boolean markProcessed(UUID eventId) {
        return processed.putIfAbsent(eventId, Boolean.TRUE) == null;
    }
}
