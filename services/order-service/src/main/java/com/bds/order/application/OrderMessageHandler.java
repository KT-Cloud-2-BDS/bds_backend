package com.bds.order.application;

import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.messaging.dto.PaymentProcessedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageHandler {

    private final OrderService orderService;

    public void processBulk(PaymentProcessedMessage message) {
        OrderStatus targetStatus = switch (message.type()) {
            case CONFIRMED -> OrderStatus.CONFIRMED;
            case REFUNDED -> OrderStatus.REFUNDED;
        };

        for (Long orderId : message.orderIds()) {
            orderService.processStatusUpdate(orderId, targetStatus);
        }
    }

    public void processPaid(Long orderId) {
        orderService.processStatusUpdate(orderId, OrderStatus.PAID);
    }

    public void processCancelled(Long orderId, String cancelReason) {
        orderService.processCancelledUpdate(orderId, cancelReason);
    }
}
