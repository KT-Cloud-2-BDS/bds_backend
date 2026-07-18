package com.bds.notification.infrastructure.messaging;

import com.bds.notification.application.NotificationService;
import com.bds.notification.application.dto.ChatNotificationMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

  private final NotificationService notificationService;

  @RabbitListener(queues = RabbitTopologyConfig.CHATTING_STATUS_QUEUE)
  public void handle(ChatNotificationMessageDto eventDto) {
    try {
      notificationService.sendChatNotification(eventDto);
    } catch (Exception e) {
      log.error("채팅 알림 처리 실패: {}", e.getMessage(), e);
    }
  }

}
