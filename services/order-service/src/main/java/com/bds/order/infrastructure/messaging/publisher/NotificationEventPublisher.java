package com.bds.order.infrastructure.messaging.publisher;

import com.bds.common.events.funding.FundingStatusChangedEvent;
import com.bds.common.events.order.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void publishStatusChanged(OrderStatusChangedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Transactional
    public void publishFundingStatusChanged(FundingStatusChangedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
