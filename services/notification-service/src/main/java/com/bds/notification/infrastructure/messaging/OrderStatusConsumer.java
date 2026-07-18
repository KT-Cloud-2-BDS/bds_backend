package com.bds.notification.infrastructure.messaging;

import com.bds.notification.application.NotificationService;
import com.bds.notification.application.dto.OrderNotificationMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusConsumer {

  private final NotificationService notificationService;

  @RabbitListener(queues = RabbitTopologyConfig.ORDER_STATUS_QUEUE)
  public void handle(
      OrderNotificationMessageDto eventDto) { // TODO: 공용 메시지 이벤트로 변경해야함. 지금은 임시로 내부 DTO로 처리
    try {
      notificationService.createOrderNotification(
          eventDto
      );
    } catch (Exception e) {
      log.error("주문 상태 알림 처리 실패: {}", e.getMessage(), e);
    }
  }

}
