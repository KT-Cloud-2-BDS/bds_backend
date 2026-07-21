package com.bds.order.infrastructure.messaging;

import com.bds.order.application.OrderMessageHandler;
import com.bds.order.infrastructure.config.OrderQueues;
import com.bds.order.infrastructure.messaging.dto.PaymentCancelledMessage;
import com.bds.order.infrastructure.messaging.dto.PaymentPaidMessage;
import com.bds.order.infrastructure.messaging.dto.PaymentProcessedMessage;
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
        PaymentProcessedMessage message = new PaymentProcessedMessage(List.of(1L, 2L), PaymentProcessedMessage.ResultType.CONFIRMED);

        rabbitTemplate.convertAndSend(OrderQueues.ORDER_EXCHANGE, "order.process", message);

        Thread.sleep(1000);
        verify(orderMessageHandler).processBulk(argThat(msg ->
                msg.type() == PaymentProcessedMessage.ResultType.CONFIRMED && msg.orderIds().size() == 2));
    }

    @Test
    void order_process_paid_큐에_메시지를_보내면_handlePaidResult가_처리한다() throws InterruptedException {
        PaymentPaidMessage message = new PaymentPaidMessage(1L);

        rabbitTemplate.convertAndSend(OrderQueues.ORDER_EXCHANGE, "order.process.paid", message);

        Thread.sleep(1000);
        verify(orderMessageHandler).processPaid(1L);
    }

    @Test
    void order_process_cancel_큐에_메시지를_보내면_handleCancelResult가_처리한다() throws InterruptedException {
        PaymentCancelledMessage message = new PaymentCancelledMessage(1L, "USER_CANCEL");

        rabbitTemplate.convertAndSend(OrderQueues.ORDER_EXCHANGE, "order.process.cancel", message);

        Thread.sleep(1000);
        verify(orderMessageHandler).processCancelled(1L, "USER_CANCEL");
    }
}