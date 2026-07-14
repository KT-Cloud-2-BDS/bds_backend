package com.bds.order.infrastructure.messaging;

import com.bds.common.events.order.OrderCreatedEvent;
import com.bds.messaging.idempotency.ProcessedEventStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCreatedListener {
    private final ProcessedEventStore processedEventStore;
    public OrderCreatedListener(ProcessedEventStore processedEventStore){
        this.processedEventStore = processedEventStore;
    }

    @RabbitListener(queues = PaymentQueues.ORDER_CREATED)
    public void handle(OrderCreatedEvent event){
        if (!processedEventStore.markProcessed(event.eventId())) {
            log.info("중복 이벤트 스킵: {}", event.eventId());
            return;   // 정상 종료 → ack
        }

        log.info("OrderCreatedEvent 수신: orderId={}, amount={}", event.orderId(), event.amount());
        // payment service process (event)
    }
}
