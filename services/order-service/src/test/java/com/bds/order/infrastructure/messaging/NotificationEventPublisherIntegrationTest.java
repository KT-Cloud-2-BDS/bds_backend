package com.bds.order.infrastructure.messaging;

import com.bds.common.events.funding.FundingStatusChangedEvent;
import com.bds.common.events.order.OrderStatusChangedEvent;
import com.bds.order.infrastructure.messaging.publisher.NotificationEventPublisher;
import com.bds.support.AbstractRabbitMQIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEventPublisherIntegrationTest extends AbstractRabbitMQIntegrationTest {

    @Autowired
    private NotificationEventPublisher notificationEventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM event_publication");
    }

    @Test
    void publishStatusChanged_호출시_outbox에_저장된다() {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.of("PAID", 1L, "테스트 펀딩", "ORD-001");

        notificationEventPublisher.publishStatusChanged(event);

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_publication WHERE event_type LIKE '%OrderStatusChangedEvent%'",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void publishFundingStatusChanged_호출시_outbox에_저장된다() {
        FundingStatusChangedEvent event = FundingStatusChangedEvent.of("FUNDING_SUCCESS", 1L, 100L);

        notificationEventPublisher.publishFundingStatusChanged(event);

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_publication WHERE event_type LIKE '%FundingStatusChangedEvent%'",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}
