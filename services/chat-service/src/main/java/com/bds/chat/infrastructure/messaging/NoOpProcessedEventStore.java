package com.bds.chat.infrastructure.messaging;

import com.bds.messaging.idempotency.ProcessedEventStore;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NoOpProcessedEventStore implements ProcessedEventStore {

    @Override
    public boolean markProcessed(UUID eventId) {
        //현재 listen을 product쪽에서만 받고 있으며, 이는 chat room쪽에서 unique로 잡아주기 때문에 현재는 true로 넘기고 있습니다.
        return true;
    }
}
