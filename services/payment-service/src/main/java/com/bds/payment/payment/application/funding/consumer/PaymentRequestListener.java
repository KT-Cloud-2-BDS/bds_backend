package com.bds.payment.payment.application.funding.consumer;

import com.bds.payment.payment.presentation.request.PaymentRequestEvent;
import com.bds.messaging.idempotency.ProcessedEventStore;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.infrastructure.config.PaymentQueues;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestListener {

    private final FundingService fundingService;
    private final ProcessedEventStore processedEventStore;

    @RabbitListener(queues = PaymentQueues.PAY_QUEUE)
    public void handle(PaymentRequestEvent event) {
        if (!processedEventStore.markProcessed(event.requestId())) {
            log.info("중복 결제 요청 스킵: requestId={}", event.requestId());
            return;
        }

        log.info("결제 요청 수신: requestId={}, orderId={}, amount={}",
                event.requestId(), event.orderId(), event.amount());

        FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                event.orderId(),
                event.memberId(),
                event.productId(),
                event.amount(),
                PaymentType.valueOf(event.paymentType())
        );

        try {
            fundingService.funding(dto);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.FUNDING_DUPLICATED) {
                log.info("이미 처리된 결제 요청 스킵: orderId={}", event.orderId());
                return;
            }
            throw e;
        }
    }
}