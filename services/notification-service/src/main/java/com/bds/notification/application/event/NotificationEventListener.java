package com.bds.notification.application.event;

import com.bds.notification.infrastructure.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final SseEmitterManager sseEmitterManager;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleNotificationCreated(NotificationCreatedEvent event) {
    if (sseEmitterManager.exist(event.memberId())) {
      sseEmitterManager.send(event.memberId(), "notification", event.payload());
    } else {
      // TODO: FCM Fallback 예정
    }
  }

}
