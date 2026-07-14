package com.bds.order.infrastructure.messaging;

import com.bds.common.events.order.OrderCreatedEvent;
import com.bds.messaging.idempotency.ProcessedEventStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Component
public class OrderCreatedListener {
    private final ProcessedEventStore processedEventStore;
    public OrderCreatedListener(ProcessedEventStore processedEventStore){
        this.processedEventStore = processedEventStore;
    }
    //이때 해당 transaction의 경우 구현한 markProcessed가 db일 경우 의미가 있습니다.
    //만약 현재 처럼 in memory 기반이라면, 다른 처리 로직이 필요합니다. (실패시 rollback할 수 있는)
    @Transactional
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
