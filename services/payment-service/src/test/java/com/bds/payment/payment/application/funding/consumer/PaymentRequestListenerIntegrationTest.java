package com.bds.payment.payment.application.funding.consumer;

import com.bds.common.events.order.OrderProcessPayEvent;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.infrastructure.config.PaymentQueues;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.support.AbstractPaymentRabbitMQIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

public class PaymentRequestListenerIntegrationTest extends AbstractPaymentRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private FundingService fundingService;

    @Test
    void order_pay_requested_큐에_메시지를_보내면_funding이_호출된다() {
        // given
        OrderProcessPayEvent message = OrderProcessPayEvent.of(
                101L, 55L, 100L, 30000L
        );

        // when
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.pay.requested",
                message
        );

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        verify(fundingService).funding(argThat((FundingPaymentRequestDto dto) ->
                                dto.orderId().equals(101L)
                                        && dto.memberId().equals(55L)
                                        && dto.productId().equals(100L)
                                        && dto.amount().equals(30000L)
                        ))
                );
    }
}