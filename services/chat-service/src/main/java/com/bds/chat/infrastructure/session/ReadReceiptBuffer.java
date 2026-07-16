package com.bds.chat.infrastructure.session;

import com.bds.chat.application.message.service.MessageService;
import com.bds.chat.domain.member.ReadReceipt;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadReceiptBuffer {

    private final ConcurrentHashMap<Key, Entry> buffer = new ConcurrentHashMap<>();
    private final MessageService messageService;

    public void mark(Long roomId, Long userId, Long lastReadMessageId, Instant readAt) {
        buffer.merge(
                new Key(roomId, userId),
                new Entry(roomId, userId, lastReadMessageId, readAt),
                (old,incoming) -> incoming.readAt().isAfter(old.readAt()) ? incoming : old
        );
    }

    @Scheduled(fixedDelayString = "${chat.read.flush-interval-ms:10000}")
    public void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        Optional.of(drain())
                .filter(batch -> !batch.isEmpty())
                .ifPresent(batch -> {
                    try {
                        messageService.upsertAllReadReceipts(batch);
                        log.debug("read receipt flush 완료: {}건", batch.size());
                    } catch (Exception e) {
                        log.error("read receipt flush 실패 — 버퍼 복원 후 다음 주기 재시도 ({}건)", batch.size(), e);
                        batch.forEach(r -> mark(r.roomId(), r.userId(), r.lastReadMessageId(), r.readAt()));
                    }
                });
    }

    @PreDestroy
    public void destroy() {
        log.info("shutdown - 잔여 read receipt flush");
        flush();
    }

    private List<ReadReceipt> drain() {
        List<ReadReceipt> batch = new ArrayList<>();
        buffer.forEach((key, value) -> {
            if (buffer.remove(key, value)) {
                batch.add(new ReadReceipt(value.roomId(), value.userId(), value.lastReadMessageId(), value.readAt()));
            }
        });
        return batch;
    }

    private record Key(Long roomId, Long userId) {}

    private record Entry(Long roomId, Long userId, Long lastReadMessageId, Instant readAt) {}
}
