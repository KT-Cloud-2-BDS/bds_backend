package com.bds.notification.infrastructure.messaging;

import com.bds.common.events.order.OrderStatusChangedEvent;
import com.bds.notification.application.NotificationService;
import com.bds.notification.application.dto.OrderNotificationMessageDto;
import com.bds.notification.common.exception.BusinessException;
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
  public void handle(OrderStatusChangedEvent eventDto) {
    try {
      notificationService.createOrderNotification(
          new OrderNotificationMessageDto(eventDto.type(), eventDto.memberId(), eventDto.fundingTitle(), eventDto.orderNo())
      );
    } catch (BusinessException e) {
      // 재시도해도 해결되지 않는 검증 실패 → 로그만 남기고 ack (DLQ 불필요)
      log.error("주문 상태 알림 처리 실패 (비재시도): {}", e.getMessage());
    } catch (Exception e) {
      log.error("주문 상태 알림 처리 실패: {}", e.getMessage(), e);
      throw e;
    }
  }

}
