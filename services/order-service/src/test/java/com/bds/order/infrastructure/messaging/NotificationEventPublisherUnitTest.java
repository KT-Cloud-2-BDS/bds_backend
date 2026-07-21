package com.bds.order.infrastructure.messaging;

import com.bds.common.events.funding.FundingStatusChangedEvent;
import com.bds.common.events.order.OrderStatusChangedEvent;
import com.bds.order.infrastructure.messaging.publisher.NotificationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherUnitTest {

    @InjectMocks
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void publishStatusChanged_호출시_이벤트를_발행한다() {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.of("PAID", 1L, "테스트 펀딩", "ORD-001");

        notificationEventPublisher.publishStatusChanged(event);

        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void publishFundingStatusChanged_호출시_이벤트를_발행한다() {
        FundingStatusChangedEvent event = FundingStatusChangedEvent.of("FUNDING_SUCCESS", 1L, 100L);

        notificationEventPublisher.publishFundingStatusChanged(event);

        verify(applicationEventPublisher).publishEvent(event);
    }
}
