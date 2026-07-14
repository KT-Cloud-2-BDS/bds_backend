package com.bds.order.infrastructure.messaging;

import com.bds.messaging.idempotency.ProcessedEventStore;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * inbox를 위한 컨슈머 중복 수신 방어(멱등성)용 임시 저장소
 * 메시지 중복 처리 방지는 강제 사항이 아니며, 각 개별 서비스의 비즈니스 요구사항에 따라 선택적으로 구현합니다
 * 기본 구현체로 로컬 맵을 제공하지만, 내부 구현은 서비스의 도메인 특성에 맞춰 자유롭게 변경할 수 있습니다.
 * (예: 다중 서버 환경을 위한 Redis 구현체 도입, 혹은 멱등성 검증이 필요 없다면 항상 true를 반환하는 더미 구현체 등)
 * 현재 해당 ttl이 존재하지 않음으로 이를 처리해줄 로직이 따로 필요합니다.
 */
@Component
public class InMemoryProcessedEventStore implements ProcessedEventStore {
    private final ConcurrentHashMap<UUID, Boolean> processed = new ConcurrentHashMap<>();

    @Override
    public boolean markProcessed(UUID eventId) {
        return processed.putIfAbsent(eventId, Boolean.TRUE) == null;
    }
}
