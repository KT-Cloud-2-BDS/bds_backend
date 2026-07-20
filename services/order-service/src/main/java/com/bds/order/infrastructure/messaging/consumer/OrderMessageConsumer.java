package com.bds.order.infrastructure.messaging.consumer;

import com.bds.order.application.OrderMessageHandler;
import com.bds.order.infrastructure.config.RabbitTopologyConfig;
import com.bds.order.infrastructure.messaging.dto.PaymentCancelledMessage;
import com.bds.order.infrastructure.messaging.dto.PaymentPaidMessage;
import com.bds.order.infrastructure.messaging.dto.PaymentProcessedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private final OrderMessageHandler orderMessageHandler;

    @RabbitListener(queues = RabbitTopologyConfig.ORDER_PROCESS_QUEUE)
    public void handleBulkResult(PaymentProcessedMessage message) {
        log.info("Received bulk message: type={}, count={}", message.type(), message.orderIds().size());
        orderMessageHandler.processBulk(message);
    }

    @RabbitListener(queues = RabbitTopologyConfig.ORDER_PROCESS_PAID_QUEUE)
    public void handlePaidResult(PaymentPaidMessage message) {
        log.info("Received paid message: orderId={}", message.orderId());
        orderMessageHandler.processPaid(message.orderId());
    }

    @RabbitListener(queues = RabbitTopologyConfig.ORDER_PROCESS_CANCEL_QUEUE)
    public void handleCancelResult(PaymentCancelledMessage message) {
        log.info("Received cancel message: orderId={}", message.orderId());
        orderMessageHandler.processCancelled(message.orderId(), message.cancelReason());
    }
}