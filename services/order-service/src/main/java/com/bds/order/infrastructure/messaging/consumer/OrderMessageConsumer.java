package com.bds.order.infrastructure.messaging.consumer;

import com.bds.common.events.payment.OrderCancelledEvent;
import com.bds.common.events.payment.OrderPaidEvent;
import com.bds.common.events.payment.OrderProcessEvent;
import com.bds.order.application.OrderMessageHandler;
import com.bds.order.infrastructure.config.OrderQueues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private final OrderMessageHandler orderMessageHandler;

    @RabbitListener(queues = OrderQueues.ORDER_PROCESS_SETTLE_QUEUE)
    public void handleBulkResult(OrderProcessEvent message) {
        log.info("Received bulk message: type={}, count={}", message.type(), message.orderIds().size());
        orderMessageHandler.processBulk(message);
    }

    @RabbitListener(queues = OrderQueues.ORDER_PROCESS_PAID_QUEUE)
    public void handlePaidResult(OrderPaidEvent message) {
        log.info("Received paid message: orderId={}", message.orderId());
        orderMessageHandler.processPaid(message.orderId());
    }

    @RabbitListener(queues = OrderQueues.ORDER_PROCESS_CANCEL_QUEUE)
    public void handleCancelResult(OrderCancelledEvent message) {
        log.info("Received cancel message: orderId={}", message.orderId());
        orderMessageHandler.processCancelled(message.orderId(), message.cancelReason());
    }
}