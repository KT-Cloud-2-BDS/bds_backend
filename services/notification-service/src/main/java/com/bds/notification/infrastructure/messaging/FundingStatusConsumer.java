package com.bds.notification.infrastructure.messaging;

import com.bds.notification.application.NotificationService;
import com.bds.notification.application.dto.FundingNotificationCommandDto;
import com.bds.notification.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingStatusConsumer {

  private final NotificationService notificationService;

  @RabbitListener(queues = RabbitTopologyConfig.FUNDING_STATUS_QUEUE)
  public void handle(
      FundingNotificationCommandDto eventDto) {// TODO: Funding 쪽에서 공용 이벤트 생성하면 그걸로 대체할 예정
    try {
      notificationService.createFundingNotification(eventDto);
    } catch (BusinessException e) {
      // 재시도해도 해결되지 않는 검증 실패 → 로그만 남기고 ack (DLQ 불필요)
      log.error("펀딩 상태 알림 처리 실패 (비재시도): {}", e.getMessage());
    } catch (Exception e) {
      log.error("펀딩 상태 알림 처리 실패: {}", e.getMessage(), e);
      throw e;
    }
  }
}
