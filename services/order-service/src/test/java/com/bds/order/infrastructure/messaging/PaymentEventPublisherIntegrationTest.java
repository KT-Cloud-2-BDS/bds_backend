package com.bds.order.infrastructure.messaging;

import com.bds.common.events.order.OrderProcessPayEvent;
import com.bds.common.events.order.OrderProcessRefundEvent;
import com.bds.common.events.order.OrderProcessSettlementEvent;
import com.bds.order.infrastructure.messaging.publisher.PaymentEventPublisher;
import com.bds.support.AbstractRabbitMQIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEventPublisherIntegrationTest extends AbstractRabbitMQIntegrationTest {

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM event_publication");
    }

    @Test
    void publishPay_호출시_outbox에_저장된다() {
        OrderProcessPayEvent event = OrderProcessPayEvent.of(1L, 1L, 1L, 30000L);

        paymentEventPublisher.publishPay(event);

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_publication WHERE event_type LIKE '%OrderProcessPayEvent%'",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void publishSettlement_호출시_outbox에_저장된다() {
        OrderProcessSettlementEvent event = OrderProcessSettlementEvent.of(
                "SETTLEMENT_CONFIRMED", 100L, 1L, List.of(
                        new OrderProcessSettlementEvent.SettlementItem(1L, 1L, 30000L)));

        paymentEventPublisher.publishSettlement(event);

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_publication WHERE event_type LIKE '%OrderProcessSettlementEvent%'",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void publishRefund_호출시_outbox에_저장된다() {
        OrderProcessRefundEvent event = OrderProcessRefundEvent.of(1L, 1L, 1L, 30000L, "USER_CANCEL");

        paymentEventPublisher.publishRefund(event);

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_publication WHERE event_type LIKE '%OrderProcessRefundEvent%'",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}
