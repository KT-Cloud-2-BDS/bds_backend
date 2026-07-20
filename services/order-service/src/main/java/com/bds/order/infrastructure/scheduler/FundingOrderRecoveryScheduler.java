package com.bds.order.infrastructure.scheduler;

import com.bds.common.events.order.PaymentProcessSettlementEvent;
import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.messaging.publisher.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingOrderRecoveryScheduler {

    private static final int CHUNK_SIZE = 500;
    private final FundingRepository fundingRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final FundingStatusUpdater fundingStatusUpdater;

    @Scheduled(cron = "0 0 2 * * *")
    public void recover() {
        log.info("[FUNDING_ORDER_RECOVERY] 배치 시작");

        LocalDateTime after = LocalDateTime.now().minusDays(3);

        List<Funding> successFundings = fundingRepository.findByStatusAndUpdatedAfter(FundingStatus.SUCCESS, after);
        for (Funding funding : successFundings) {
            try {
                if (funding.getType() == FundingType.INSTANT) {
                    recoverInstantSuccess(funding);
                } else {
                    recoverReservedSuccess(funding);
                }
            } catch (Exception e) {
                log.error("[FUNDING_ORDER_RECOVERY] SUCCESS 펀딩 복구 실패: fundingId={}, exceptionType={}, reason={}",
                        funding.getId(), e.getClass().getSimpleName(), e.getMessage());
            }
        }

        List<Funding> failedFundings = fundingRepository.findByStatusAndUpdatedAfter(FundingStatus.FAILED, after);
        for (Funding funding : failedFundings) {
            try {
                if (funding.getType() == FundingType.INSTANT) {
                    recoverInstantFailure(funding);
                } else {
                    recoverReservedFailure(funding);
                }
            } catch (Exception e) {
                log.error("[FUNDING_ORDER_RECOVERY] FAILED 펀딩 복구 실패: fundingId={}, exceptionType={}, reason={}",
                        funding.getId(), e.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.info("[FUNDING_ORDER_RECOVERY] 배치 완료");
    }

    private void recoverInstantSuccess(Funding funding) {
        fundingStatusUpdater.handleFundingSuccess(funding.getId(), funding.getCreatorId());
    }

    private void recoverInstantFailure(Funding funding) {
        publishSettlementForStatus(funding.getId(), funding.getCreatorId(), OrderStatus.CANCELLED, "FUNDING_FAILED_REFUND");
        fundingStatusUpdater.handleFundingFailure(funding.getId(), funding.getCreatorId());
    }

    private void recoverReservedSuccess(Funding funding) {
        publishSettlementForStatus(funding.getId(), funding.getCreatorId(), OrderStatus.PAYING, "RESERVED_FUNDING_CONFIRMED");
        fundingStatusUpdater.handleReservedFundingSuccess(funding.getId(), funding.getCreatorId());
    }

    private void recoverReservedFailure(Funding funding) {
        fundingStatusUpdater.handleReservedFundingFailure(funding.getId());
    }

    private void publishSettlementForStatus(Long fundingId, Long creatorMemberId, OrderStatus status, String settlementType) {
        Long lastOrderId = 0L;
        while (true) {
            List<Long> orderIds = orderRepository.findOrderIdsByFundingIdAndStatus(fundingId, status, lastOrderId, CHUNK_SIZE);
            if (orderIds.isEmpty()) break;

            List<PaymentProcessSettlementEvent.SettlementItem> items = new ArrayList<>();
            for (Long orderId : orderIds) {
                try {
                    orderService.createSettlementItem(orderId).ifPresent(items::add);
                } catch (Exception e) {
                    log.error("[FUNDING_ORDER_RECOVERY] publishSettlementForStatus failed: orderId={}, exceptionType={}, reason={}",
                            orderId, e.getClass().getSimpleName(), e.getMessage());
                }
            }

            if (!items.isEmpty()) {
                paymentEventPublisher.publishSettlement(
                        PaymentProcessSettlementEvent.of(settlementType, creatorMemberId, items));
            }

            lastOrderId = orderIds.get(orderIds.size() - 1);
            if (orderIds.size() < CHUNK_SIZE) break;
        }
    }
}