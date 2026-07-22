package com.bds.payment.payment.application.funding.consumer;

import com.bds.common.events.order.OrderProcessSettlementEvent;
import com.bds.common.events.order.OrderProcessSettlementEvent.SettlementItem;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.domain.common.SettlementType;
import com.bds.payment.payment.infrastructure.config.PaymentQueues;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.bds.payment.support.AbstractPaymentRabbitMQIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

class SettlementRequestListenerIntegrationTest extends AbstractPaymentRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private FundingService fundingService;

    @Nested
    class SettlementConfirmed {

        @Test
        void SETTLEMENT_CONFIRMED_타입이면_confirmSettlement가_호출된다() {
            OrderProcessSettlementEvent message = OrderProcessSettlementEvent.of(
                    "SETTLEMENT_CONFIRMED",
                    5L,
                    100L,
                    List.of(new SettlementItem(101L, null, 30000L))
            );

            rabbitTemplate.convertAndSend(
                    PaymentQueues.ORDER_EXCHANGE,
                    "order.settle.requested",
                    message
            );

            await().atMost(Duration.ofSeconds(5))
                    .untilAsserted(() ->
                            verify(fundingService).confirmSettlement(argThat((SettlementBatchRequestDto dto) ->
                                    dto.type() == SettlementType.SETTLEMENT_CONFIRMED
                                            && dto.creatorMemberId().equals(5L)
                                            && dto.items().size() == 1
                            ))
                    );
        }
    }

    @Nested
    class ReservedFundingConfirmed {

        @Test
        void RESERVED_FUNDING_CONFIRMED_타입이면_confirmReservedFunding이_호출된다() {
            OrderProcessSettlementEvent message = OrderProcessSettlementEvent.of(
                    "RESERVED_FUNDING_CONFIRMED",
                    5L,
                    100L,
                    List.of(new SettlementItem(301L, 70L, 30000L))
            );

            rabbitTemplate.convertAndSend(
                    PaymentQueues.ORDER_EXCHANGE,
                    "order.settle.requested",
                    message
            );

            await().atMost(Duration.ofSeconds(5))
                    .untilAsserted(() ->
                            verify(fundingService).confirmReservedFunding(argThat((SettlementBatchRequestDto dto) ->
                                    dto.type() == SettlementType.RESERVED_FUNDING_CONFIRMED
                                            && dto.items().getFirst().memberId().equals(70L)
                            ))
                    );
        }
    }

    @Nested
    class FundingFailedRefund {

        @Test
        void FUNDING_FAILED_REFUND_타입이면_refundFailedFunding이_호출된다() {
            OrderProcessSettlementEvent message = OrderProcessSettlementEvent.of(
                    "FUNDING_FAILED_REFUND",
                    null,
                    100L,
                    List.of(new SettlementItem(201L, 55L, 30000L))
            );

            rabbitTemplate.convertAndSend(
                    PaymentQueues.ORDER_EXCHANGE,
                    "order.settle.requested",
                    message
            );

            await().atMost(Duration.ofSeconds(5))
                    .untilAsserted(() ->
                            verify(fundingService).refundFailedFunding(argThat((SettlementBatchRequestDto dto) ->
                                    dto.type() == SettlementType.FUNDING_FAILED_REFUND
                                            && dto.items().getFirst().memberId().equals(55L)
                            ))
                    );
        }
    }
}