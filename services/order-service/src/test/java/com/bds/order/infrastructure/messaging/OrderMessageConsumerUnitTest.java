package com.bds.order.infrastructure.messaging;

import com.bds.order.application.OrderMessageHandler;
import com.bds.order.infrastructure.messaging.consumer.OrderMessageConsumer;
import com.bds.order.infrastructure.messaging.dto.PaymentCancelledMessage;
import com.bds.order.infrastructure.messaging.dto.PaymentPaidMessage;
import com.bds.order.infrastructure.messaging.dto.PaymentProcessedMessage;
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
        PaymentProcessedMessage message = new PaymentProcessedMessage(List.of(1L, 2L), PaymentProcessedMessage.ResultType.CONFIRMED);

        orderMessageConsumer.handleBulkResult(message);

        verify(orderMessageHandler).processBulk(message);
    }

    @Test
    void handlePaidResult_수신시_processPaid를_호출한다() {
        PaymentPaidMessage message = new PaymentPaidMessage(1L);

        orderMessageConsumer.handlePaidResult(message);

        verify(orderMessageHandler).processPaid(1L);
    }

    @Test
    void handleCancelResult_수신시_processCancelled를_호출한다() {
        PaymentCancelledMessage message = new PaymentCancelledMessage(1L, "USER_CANCEL");

        orderMessageConsumer.handleCancelResult(message);

        verify(orderMessageHandler).processCancelled(1L, "USER_CANCEL");
    }
}