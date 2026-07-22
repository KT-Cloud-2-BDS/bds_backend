package com.bds.order.infrastructure.messaging.publisher;

import com.bds.common.events.order.OrderProcessPayEvent;
import com.bds.common.events.order.OrderProcessRefundEvent;
import com.bds.common.events.order.OrderProcessSettlementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void publishPay(OrderProcessPayEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Transactional
    public void publishSettlement(OrderProcessSettlementEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Transactional
    public void publishRefund(OrderProcessRefundEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
