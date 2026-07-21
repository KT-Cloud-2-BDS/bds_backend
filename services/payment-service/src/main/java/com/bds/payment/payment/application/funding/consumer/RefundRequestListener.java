package com.bds.payment.payment.application.funding.consumer;

import com.bds.payment.payment.presentation.request.RefundRequestEvent;
import com.bds.messaging.idempotency.ProcessedEventStore;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.infrastructure.config.PaymentQueues;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRequestListener {

    private final FundingService fundingService;
    private final ProcessedEventStore processedEventStore;

    @RabbitListener(queues = PaymentQueues.REFUND_QUEUE)
    public void handle(RefundRequestEvent event) {
        if (!processedEventStore.markProcessed(event.requestId())) {
            log.info("중복 환불 요청 스킵: requestId={}", event.requestId());
            return;
        }

        log.info("환불 요청 수신: requestId={}, orderId={}",
                event.requestId(), event.orderId());

        RefundRequestDto dto = new RefundRequestDto(
                event.requestId(),
                event.orderId(),
                event.memberId(),
                event.amount(),
                event.cancelReason()
        );

        try {
            fundingService.refund(dto);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.FUNDING_ALREADY_REFUNDED) {
                log.info("이미 환불된 요청 스킵: orderId={}", event.orderId());
                return;
            }
            throw e;
        }
    }
}