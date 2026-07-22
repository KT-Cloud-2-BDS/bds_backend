package com.bds.order.infrastructure.messaging;

import com.bds.common.events.payment.OrderCancelledEvent;
import com.bds.common.events.payment.OrderPaidEvent;
import com.bds.common.events.payment.OrderProcessEvent;
import com.bds.order.application.OrderMessageHandler;
import com.bds.order.infrastructure.messaging.consumer.OrderMessageConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderMessageConsumerUnitTest {

    @InjectMocks
    private OrderMessageConsumer orderMessageConsumer;

    @Mock
    private OrderMessageHandler orderMessageHandler;

    @Test
    void handleBulkResult_수신시_processBulk를_호출한다() {
        OrderProcessEvent message = OrderProcessEvent.confirmed(List.of(1L, 2L));

        orderMessageConsumer.handleBulkResult(message);

        verify(orderMessageHandler).processBulk(message);
    }

    @Test
    void handlePaidResult_수신시_processPaid를_호출한다() {
        OrderPaidEvent message = OrderPaidEvent.of(1L);

        orderMessageConsumer.handlePaidResult(message);

        verify(orderMessageHandler).processPaid(1L);
    }

    @Test
    void handleCancelResult_수신시_processCancelled를_호출한다() {
        OrderCancelledEvent message = OrderCancelledEvent.of(1L, "USER_CANCEL");

        orderMessageConsumer.handleCancelResult(message);

        verify(orderMessageHandler).processCancelled(1L, "USER_CANCEL");
    }
}