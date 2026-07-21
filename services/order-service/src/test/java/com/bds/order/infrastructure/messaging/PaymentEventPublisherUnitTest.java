package com.bds.order.infrastructure.messaging;

import com.bds.common.events.order.OrderProcessPayEvent;
import com.bds.common.events.order.OrderProcessRefundEvent;
import com.bds.common.events.order.OrderProcessSettlementEvent;
import com.bds.order.infrastructure.messaging.publisher.PaymentEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherUnitTest {

    @InjectMocks
    private PaymentEventPublisher paymentEventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void publishPay_호출시_이벤트를_발행한다() {
        OrderProcessPayEvent event = OrderProcessPayEvent.of(1L, 1L, 1L, 30000L);

        paymentEventPublisher.publishPay(event);

        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void publishSettlement_호출시_이벤트를_발행한다() {
        OrderProcessSettlementEvent event = OrderProcessSettlementEvent.of(
                "SETTLEMENT_CONFIRMED", 100L, 1L, List.of(
                        new OrderProcessSettlementEvent.SettlementItem(1L, 1L, 30000L)));

        paymentEventPublisher.publishSettlement(event);

        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void publishRefund_호출시_이벤트를_발행한다() {
        OrderProcessRefundEvent event = OrderProcessRefundEvent.of(1L, 1L, 1L, 30000L, "USER_CANCEL");

        paymentEventPublisher.publishRefund(event);

        verify(applicationEventPublisher).publishEvent(event);
    }
}
