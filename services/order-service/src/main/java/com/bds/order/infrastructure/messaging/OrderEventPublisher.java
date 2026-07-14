package com.bds.order.infrastructure.messaging;

import com.bds.common.events.order.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
public class OrderEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    public OrderEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    @Transactional
    public void OrderCreated(Long orderId, Long amount){
        log.info("OrderCreatedEvent : orderId={}, amount={}", orderId, amount);
        applicationEventPublisher.publishEvent(OrderCreatedEvent.of(orderId, amount));
    }
}
