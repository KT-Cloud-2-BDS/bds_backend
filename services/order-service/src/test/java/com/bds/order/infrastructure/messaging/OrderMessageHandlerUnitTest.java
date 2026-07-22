package com.bds.order.infrastructure.messaging;

import com.bds.common.events.payment.OrderProcessEvent;
import com.bds.order.application.OrderMessageHandler;
import com.bds.order.application.OrderService;
import com.bds.order.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderMessageHandlerUnitTest {

    @InjectMocks
    private OrderMessageHandler orderMessageHandler;

    @Mock
    private OrderService orderService;

    @Nested
    @DisplayName("processBulk")
    class ProcessBulk {

        @Test
        void CONFIRMED_타입이면_각_주문을_CONFIRMED로_업데이트한다() {
            OrderProcessEvent message = OrderProcessEvent.confirmed(List.of(1L, 2L));

            orderMessageHandler.processBulk(message);

            verify(orderService).processStatusUpdate(1L, OrderStatus.CONFIRMED);
            verify(orderService).processStatusUpdate(2L, OrderStatus.CONFIRMED);
        }

        @Test
        void REFUNDED_타입이면_각_주문을_REFUNDED로_업데이트한다() {
            OrderProcessEvent message = OrderProcessEvent.refunded(List.of(3L, 4L));

            orderMessageHandler.processBulk(message);

            verify(orderService).processStatusUpdate(3L, OrderStatus.REFUNDED);
            verify(orderService).processStatusUpdate(4L, OrderStatus.REFUNDED);
        }
    }

    @Nested
    @DisplayName("processPaid")
    class ProcessPaid {

        @Test
        void 주문을_PAID로_업데이트한다() {
            orderMessageHandler.processPaid(1L);

            verify(orderService).processStatusUpdate(1L, OrderStatus.PAID);
        }
    }

    @Nested
    @DisplayName("processCancelled")
    class ProcessCancelled {

        @Test
        void 주문을_취소_업데이트한다() {
            orderMessageHandler.processCancelled(1L, "USER_CANCEL");

            verify(orderService).processCancelledUpdate(1L, "USER_CANCEL");
        }
    }
}