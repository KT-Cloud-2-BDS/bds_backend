package com.bds.payment.payment.application.funding.consumer;

import com.bds.common.events.order.OrderProcessPayEvent;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.infrastructure.config.PaymentQueues;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.support.AbstractPaymentRabbitMQIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PaymentRequestListenerExceptionIntegrationTest extends AbstractPaymentRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private FundingService fundingService;

    @Test
    void 동일한_requestId로_두번_발행하면_두번째는_스킵된다() {
        // given
        OrderProcessPayEvent message = OrderProcessPayEvent.of(
                201L, 55L, 100L, 30000L
        );

        // when — 같은 이벤트 두 번 발행 (같은 requestId)
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.pay.requested",
                message
        );
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.pay.requested",
                message
        );

        // then — funding은 정확히 한 번만 호출되어야 함
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        verify(fundingService, times(1)).funding(any(FundingPaymentRequestDto.class))
                );

        // 추가 검증: 두 번째 호출이 지연되어 오는지 확인 (5초 더 대기해도 여전히 1번)
        verify(fundingService, after(2000).times(1)).funding(any(FundingPaymentRequestDto.class));
    }

    @Test
    void FUNDING_DUPLICATED_예외는_정상_종료로_처리된다() {
        // given
        OrderProcessPayEvent message = OrderProcessPayEvent.of(
                202L, 55L, 100L, 30000L
        );
        doThrow(new BusinessException(ErrorCode.FUNDING_DUPLICATED))
                .when(fundingService).funding(any(FundingPaymentRequestDto.class));

        // when
        rabbitTemplate.convertAndSend(
                PaymentQueues.ORDER_EXCHANGE,
                "order.pay.requested",
                message
        );

        // then — 예외가 났지만 로그만 남기고 정상 종료 (재시도 발생 안 함)
        // funding은 정확히 1번만 호출되고, 재시도로 인한 추가 호출이 없어야 함
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        verify(fundingService, times(1)).funding(any(FundingPaymentRequestDto.class))
                );

        // 2초 더 기다려도 추가 호출 없어야 함 (DLQ 재시도 없음)
        verify(fundingService, after(2000).times(1)).funding(any(FundingPaymentRequestDto.class));
    }
}