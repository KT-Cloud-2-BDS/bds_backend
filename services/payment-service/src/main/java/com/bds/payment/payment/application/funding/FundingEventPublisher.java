package com.bds.payment.payment.application.funding;

import com.bds.common.events.payment.OrderCancelledEvent;
import com.bds.common.events.payment.OrderPaidEvent;
import com.bds.common.events.payment.OrderProcessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void publishOrderPaid(Long orderId) {
        log.info("publishOrderPaid: orderId={}", orderId);
        eventPublisher.publishEvent(OrderPaidEvent.of(orderId));
    }

    @Transactional
    public void publishOrderCancelled(Long orderId, String cancelReason) {
        log.info("publishOrderCancelled: orderId={}, reason={}", orderId, cancelReason);
        eventPublisher.publishEvent(OrderCancelledEvent.of(orderId, cancelReason));
    }

    @Transactional
    public void publishOrderProcessConfirmed(List<Long> orderIds) {
        log.info("publishOrderProcessConfirmed: orderIds={}", orderIds);
        eventPublisher.publishEvent(OrderProcessEvent.confirmed(orderIds));
    }

    @Transactional
    public void publishOrderProcessRefunded(List<Long> orderIds) {
        log.info("publishOrderProcessRefunded: orderIds={}", orderIds);
        eventPublisher.publishEvent(OrderProcessEvent.refunded(orderIds));
    }
}
