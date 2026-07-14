package com.bds.common.events.notification;

import org.springframework.modulith.events.Externalized;
import java.time.Instant;
import java.util.UUID;

/*
 * 스키마 호환 규칙: 필드 추가만 허용, 삭제/타입 변경 금지.
 * @Externalized("notification.exchange::notification.created")
                     Exchange 이름             Routing Key
 */
@Externalized("notification.exchange::notification.created")
public record BaseNotificationEvent(
        UUID eventId,        // 컨슈머 멱등성 체크 키
        Long receiverId,
        Long senderId,
        Long roomId,
        String content,
        Instant occurredAt
) {
    public static BaseNotificationEvent of(
            Long receiverId,
            Long senderId,
            Long roomId,
            String content){
        return new BaseNotificationEvent(
                UUID.randomUUID(),
                receiverId,
                senderId,
                roomId,
                content,
                Instant.now()
        );
    }
}
