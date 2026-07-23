package com.bds.order.infrastructure.messaging;

import com.bds.common.events.payment.OrderCancelledEvent;
import com.bds.common.events.payment.OrderPaidEvent;
import com.bds.common.events.payment.OrderProcessEvent;
import com.bds.order.application.OrderMessageHandler;
import com.bds.order.infrastructure.config.OrderQueues;
import com.bds.support.AbstractRabbitMQIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;


class OrderMessageConsumerIntegrationTest extends AbstractRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private OrderMessageHandler orderMessageHandler;

    @Test
    void order_process_큐에_메시지를_보내면_handleBulkResult가_처리한다() throws InterruptedException {
        OrderProcessEvent message = OrderProcessEvent.confirmed(List.of(1L, 2L));

        rabbitTemplate.convertAndSend(OrderQueues.PAYMENT_EXCHANGE, "payment.settled", message);

        Thread.sleep(1000);
        verify(orderMessageHandler).processBulk(argThat(msg ->
                msg.type().equals("CONFIRMED") && msg.orderIds().size() == 2));
    }

    @Test
    void order_process_paid_큐에_메시지를_보내면_handlePaidResult가_처리한다() throws InterruptedException {
        OrderPaidEvent message = OrderPaidEvent.of(1L);

        rabbitTemplate.convertAndSend(OrderQueues.PAYMENT_EXCHANGE, "payment.paid", message);

        Thread.sleep(1000);
        verify(orderMessageHandler).processPaid(1L);
    }

    @Test
    void order_process_cancel_큐에_메시지를_보내면_handleCancelResult가_처리한다() throws InterruptedException {
        OrderCancelledEvent message = OrderCancelledEvent.of(1L, "USER_CANCEL");

        rabbitTemplate.convertAndSend(OrderQueues.PAYMENT_EXCHANGE, "payment.cancelled", message);

        Thread.sleep(1000);
        verify(orderMessageHandler).processCancelled(1L, "USER_CANCEL");
    }
}