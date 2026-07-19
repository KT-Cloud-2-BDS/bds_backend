package com.bds.payment.payment.application.funding.consumer;

import com.bds.common.events.payment.SettlementRequestEvent;
import com.bds.messaging.idempotency.ProcessedEventStore;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.domain.common.SettlementType;
import com.bds.payment.payment.infrastructure.config.PaymentQueues;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementRequestListener {

    private final FundingService fundingService;
    private final ProcessedEventStore processedEventStore;

    @RabbitListener(queues = PaymentQueues.SETTLEMENT_QUEUE)
    public void handle(SettlementRequestEvent event) {
        if (!processedEventStore.markProcessed(event.batchId())) {
            log.info("중복 정산 배치 스킵: batchId={}", event.batchId());
            return;
        }

        log.info("정산 배치 수신: batchId={}, type={}, itemCount={}",
                event.batchId(), event.type(), event.items().size());

        List<SettlementItem> items = event.items().stream()
                .map(i -> new SettlementItem(i.orderId(), i.memberId(), i.amount()))
                .toList();

        SettlementBatchRequestDto dto = new SettlementBatchRequestDto(
                event.batchId(),
                SettlementType.valueOf(event.type()),
                event.creatorMemberId(),
                event.productId(),
                items
        );

        switch (event.type()) {
            case "SETTLEMENT_CONFIRMED" -> fundingService.confirmSettlement(dto);
            case "RESERVED_FUNDING_CONFIRMED" -> fundingService.confirmReservedFunding(dto);
            case "FUNDING_FAILED_REFUND" -> fundingService.refundFailedFunding(dto);
            default -> log.error("Unknown settlement type: {}", event.type());
        }
    }
}