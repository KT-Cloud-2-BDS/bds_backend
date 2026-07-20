package com.bds.order.infrastructure.messaging.publisher;

import com.bds.common.events.order.PaymentProcessPayEvent;
import com.bds.common.events.order.PaymentProcessRefundEvent;
import com.bds.common.events.order.PaymentProcessSettlementEvent;
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
    public void publishPay(PaymentProcessPayEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Transactional
    public void publishSettlement(PaymentProcessSettlementEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Transactional
    public void publishRefund(PaymentProcessRefundEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
