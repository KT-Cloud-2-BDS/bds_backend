package com.bds.payment.payment.application.funding.consumer;

import com.bds.common.events.order.OrderProcessRefundEvent;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.infrastructure.config.PaymentQueues;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import com.bds.payment.support.AbstractPaymentRabbitMQIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RefundRequestListenerExceptionIntegrationTest extends AbstractPaymentRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private FundingService fundingService;

    @Test
    void 동일한_requestId로_두번_발행하면_두번째는_스킵된다() {
        // given
        OrderProcessRefundEvent message = OrderProcessRefundEvent.of(
                301L, 55L, 100L, 30000L, "USER_CANCEL"
        );

        // when — 같은 이벤트 두 번 발행
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.refund.requested",
                message
        );
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.refund.requested",
                message
        );

        // then — refund는 정확히 한 번만 호출됨
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        verify(fundingService, times(1)).refund(any(RefundRequestDto.class))
                );
        verify(fundingService, after(2000).times(1)).refund(any(RefundRequestDto.class));
    }

    @Test
    void FUNDING_ALREADY_REFUNDED_예외는_정상_종료로_처리된다() {
        // given
        OrderProcessRefundEvent message = OrderProcessRefundEvent.of(
                302L, 55L, 100L, 30000L, "USER_CANCEL"
        );
        doThrow(new BusinessException(ErrorCode.FUNDING_ALREADY_REFUNDED))
                .when(fundingService).refund(any(RefundRequestDto.class));

        // when
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.refund.requested",
                message
        );

        // then — 예외가 발생했지만 재시도 없이 정상 종료
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        verify(fundingService, times(1)).refund(any(RefundRequestDto.class))
                );
        verify(fundingService, after(2000).times(1)).refund(any(RefundRequestDto.class));
    }
}