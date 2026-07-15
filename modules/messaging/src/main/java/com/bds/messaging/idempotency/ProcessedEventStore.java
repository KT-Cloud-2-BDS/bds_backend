package com.bds.messaging.idempotency;

import java.util.UUID;

public interface ProcessedEventStore {
    boolean markProcessed(UUID eventId);
}
