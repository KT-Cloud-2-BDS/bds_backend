package com.bds.payment.payment.application.funding.consumer;

import com.bds.common.events.order.OrderProcessRefundEvent;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.infrastructure.config.PaymentQueues;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import com.bds.payment.support.AbstractPaymentRabbitMQIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

public class RefundRequestListenerIntegrationTest extends AbstractPaymentRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private FundingService fundingService;

    @Test
    void order_refund_requested_큐에_메시지를_보내면_refund가_호출된다() {
        // given
        OrderProcessRefundEvent message = OrderProcessRefundEvent.of(
                101L,           // orderId
                55L,            // memberId
                100L,           // fundingId
                30000L,         // amount
                "USER_CANCEL"   // cancelReason
        );

        // when
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.refund.requested",
                message
        );

        // then
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        verify(fundingService).refund(argThat((RefundRequestDto dto) ->
                                dto.orderId().equals(101L)
                                        && dto.memberId().equals(55L)
                                        && dto.productId().equals(100L)
                                        && dto.amount().equals(30000L)
                                        && dto.cancelReason().equals("USER_CANCEL")
                        ))
                );
    }
}