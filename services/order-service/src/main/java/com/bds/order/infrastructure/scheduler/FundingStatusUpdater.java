package com.bds.order.infrastructure.scheduler;


import com.bds.common.events.order.PaymentProcessSettlementEvent;
import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.funding.JudgmentOutcome;
import com.bds.order.infrastructure.funding.JudgmentType;
import com.bds.order.infrastructure.messaging.publisher.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingStatusUpdater {

    private static final int CHUNK_SIZE = 500;
    private final FundingRepository fundingRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public JudgmentOutcome judgeFunding(Long fundingId) {
        Funding funding = fundingRepository.findByIdForUpdate(fundingId)
                .orElseThrow(() -> new IllegalStateException(
                        "[FUNDING_JUDGE] Funding not found: fundingId=" + fundingId));

        if (funding.getStatus() != FundingStatus.ACTIVE && funding.getStatus() != FundingStatus.HOLDING) {
            throw new IllegalStateException(
                    "[FUNDING_JUDGE] 판정 불가 상태: fundingId=" + fundingId + ", status=" + funding.getStatus());
        }

        boolean isSuccess = funding.getCurrentAmount() >= funding.getGoalAmount();

        if (isSuccess) {
            // TODO: Funding 성공 Notification Message 발행
            funding.markSuccess();
        } else {
            // TODO: Funding 실패 Notification Message 발행
            funding.markFailed();
        }

        fundingRepository.save(funding);
        log.info("[FUNDING_JUDGE] 펀딩 {} - fundingId: {}", isSuccess ? "성공" : "실패", funding.getId());

        JudgmentType type;
        if (funding.getType() == FundingType.RESERVED) {
            type = isSuccess ? JudgmentType.RESERVED_SUCCESS : JudgmentType.RESERVED_FAILURE;
        } else {
            type = isSuccess ? JudgmentType.INSTANT_SUCCESS : JudgmentType.INSTANT_FAILURE;
        }

        return new JudgmentOutcome(type, funding.getCreatorId());
    }

    public void handleFundingSuccess(Long fundingId, Long creatorMemberId) {
        Long lastOrderId = 0L;
        while (true) {
            List<Long> paidOrderIds = orderRepository.findOrderIdsByFundingIdAndStatus(fundingId, OrderStatus.PAID, lastOrderId, CHUNK_SIZE);
            if (paidOrderIds.isEmpty()) break;

            List<PaymentProcessSettlementEvent.SettlementItem> items = new ArrayList<>();
            for (Long orderId : paidOrderIds) {
                try {
                    orderService.createSettlementItem(orderId).ifPresent(items::add);
                } catch (Exception e) {
                    log.error("[FundingStatusUpdater] handleFundingSuccess failed: orderId={}, exceptionType={}, reason={}",
                            orderId, e.getClass().getSimpleName(), e.getMessage());
                }
            }

            if (!items.isEmpty()) {
                paymentEventPublisher.publishSettlement(
                        PaymentProcessSettlementEvent.of("SETTLEMENT_CONFIRMED", creatorMemberId, items));
            }

            lastOrderId = paidOrderIds.get(paidOrderIds.size() - 1);
            if (paidOrderIds.size() < CHUNK_SIZE) break;
        }

        log.info("[FUNDING_JUDGE] 즉시 펀딩 성공 주문 처리 완료 - fundingId: {}", fundingId);
    }

    public void handleReservedFundingSuccess(Long fundingId, Long creatorMemberId) {
        Long lastOrderId = 0L;
        while (true) {
            List<Long> reservedOrderIds = orderRepository.findOrderIdsByFundingIdAndStatus(fundingId, OrderStatus.RESERVED, lastOrderId, CHUNK_SIZE);
            if (reservedOrderIds.isEmpty()) break;

            List<PaymentProcessSettlementEvent.SettlementItem> items = new ArrayList<>();
            for (Long orderId : reservedOrderIds) {
                try {
                    orderService.processReservedFundingConfirmed(orderId).ifPresent(items::add);
                } catch (Exception e) {
                    log.error("[FundingStatusUpdater] handleReservedFundingSuccess failed: orderId={}, exceptionType={}, reason={}",
                            orderId, e.getClass().getSimpleName(), e.getMessage());
                }
            }

            if (!items.isEmpty()) {
                paymentEventPublisher.publishSettlement(
                        PaymentProcessSettlementEvent.of("RESERVED_FUNDING_CONFIRMED", creatorMemberId, items));
            }

            lastOrderId = reservedOrderIds.get(reservedOrderIds.size() - 1);
            if (reservedOrderIds.size() < CHUNK_SIZE) break;
        }

        log.info("[FUNDING_JUDGE] 예약 펀딩 성공 주문 처리 완료 - fundingId: {}", fundingId);
    }

    public void handleFundingFailure(Long fundingId, Long creatorMemberId) {
        Long lastOrderId = 0L;
        while (true) {
            List<Long> paidOrderIds = orderRepository.findOrderIdsByFundingIdAndStatus(fundingId, OrderStatus.PAID, lastOrderId, CHUNK_SIZE);
            if (paidOrderIds.isEmpty()) break;

            List<PaymentProcessSettlementEvent.SettlementItem> items = new ArrayList<>();
            for (Long orderId : paidOrderIds) {
                try {
                    orderService.processFundingFailedRefund(orderId).ifPresent(items::add);
                } catch (Exception e) {
                    log.error("[FundingStatusUpdater] handleFundingFailure failed: orderId={}, exceptionType={}, reason={}",
                            orderId, e.getClass().getSimpleName(), e.getMessage());
                }
            }

            if (!items.isEmpty()) {
                paymentEventPublisher.publishSettlement(
                        PaymentProcessSettlementEvent.of("FUNDING_FAILED_REFUND", creatorMemberId, items));
            }

            lastOrderId = paidOrderIds.get(paidOrderIds.size() - 1);
            if (paidOrderIds.size() < CHUNK_SIZE) break;
        }

        log.info("[FUNDING_JUDGE] 즉시 펀딩 실패 주문 처리 완료 - fundingId: {}", fundingId);
    }

    public void handleReservedFundingFailure(Long fundingId) {
        Long lastOrderId = 0L;
        while (true) {
            List<Long> reservedOrderIds = orderRepository.findOrderIdsByFundingIdAndStatus(fundingId, OrderStatus.RESERVED, lastOrderId, CHUNK_SIZE);
            if (reservedOrderIds.isEmpty()) break;

            for (Long orderId : reservedOrderIds) {
                try {
                    orderService.processCancelledUpdate(orderId, CancelReason.FUNDING_FAILED.name());
                } catch (Exception e) {
                    log.error("[FundingStatusUpdater] handleReservedFundingFailure failed: orderId={}, exceptionType={}, reason={}",
                            orderId, e.getClass().getSimpleName(), e.getMessage());
                }
            }

            lastOrderId = reservedOrderIds.get(reservedOrderIds.size() - 1);
            if (reservedOrderIds.size() < CHUNK_SIZE) break;
        }

        log.info("[FUNDING_JUDGE] 예약 펀딩 실패 주문 처리 완료 - fundingId: {}", fundingId);
    }

    @Transactional
    public void activateFunding(Long fundingId) {
        Funding funding = fundingRepository.findByIdForUpdate(fundingId)
                .orElseThrow(() -> new IllegalStateException(
                        "[FUNDING_ACTIVATE] Funding not found: fundingId=" + fundingId));

        if (funding.getStatus() != FundingStatus.SCHEDULED) {
            log.warn("[FUNDING_ACTIVATE] 활성화 불가 상태: fundingId={}, status={}", fundingId, funding.getStatus());
            return;
        }

        funding.activate();
        fundingRepository.save(funding);
        log.info("[FUNDING_ACTIVATE] 펀딩 활성화 - fundingId: {}", fundingId);
    }
}
