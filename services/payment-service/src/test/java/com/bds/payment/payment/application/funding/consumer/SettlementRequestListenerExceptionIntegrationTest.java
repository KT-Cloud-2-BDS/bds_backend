package com.bds.payment.payment.application.funding.consumer;

import com.bds.common.events.order.OrderProcessSettlementEvent;
import com.bds.common.events.order.OrderProcessSettlementEvent.SettlementItem;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.infrastructure.config.PaymentQueues;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.bds.payment.support.AbstractPaymentRabbitMQIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SettlementRequestListenerExceptionIntegrationTest extends AbstractPaymentRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private FundingService fundingService;

    @Test
    void 동일한_batchId로_두번_발행하면_두번째는_스킵된다() {
        // given
        OrderProcessSettlementEvent message = OrderProcessSettlementEvent.of(
                "SETTLEMENT_CONFIRMED",
                5L,
                100L,
                List.of(new SettlementItem(401L, null, 30000L))
        );

        // when — 같은 batchId 이벤트 두 번 발행
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.settle.requested",
                message
        );
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.settle.requested",
                message
        );

        // then — confirmSettlement는 정확히 한 번만 호출
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        verify(fundingService, times(1)).confirmSettlement(any(SettlementBatchRequestDto.class))
                );
        verify(fundingService, after(2000).times(1)).confirmSettlement(any(SettlementBatchRequestDto.class));
    }
}