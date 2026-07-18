package com.bds.order.infrastructure.scheduler;


import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingStatusUpdater {

    private final FundingRepository fundingRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    private final int CHUNK_SIZE = 500;

    @Transactional
    public boolean judgeFunding(Long fundingId) {
        Funding funding = fundingRepository.findByIdForUpdate(fundingId)
                .orElseThrow(() -> new IllegalStateException(
                        "[FUNDING_JUDGE] Funding not found: fundingId=" + fundingId));

        if (funding.getStatus() != FundingStatus.ACTIVE && funding.getStatus() != FundingStatus.HOLDING) {
            throw new IllegalStateException(
                    "[FUNDING_JUDGE] 판정 불가 상태: fundingId=" + fundingId + ", status=" + funding.getStatus());
        }

        boolean isSuccess = funding.getCurrentAmount() >= funding.getGoalAmount();

        if (isSuccess) {
            funding.markSuccess();
        } else {
            funding.markFailed();
        }

        fundingRepository.save(funding);

        return isSuccess;
    }


    public void handleFundingSuccess(Long fundingId) {
        // TODO: Funding 성공 Notification Message 발행

        int chunkSize = 500;

        // 즉시 주문 (PAID) → 정산 요청 발행
        Long lastOrderId = 0L;
        while (true) {
            List<Long> paidOrderIds = orderRepository.findOrderIdsByFundingIdAndStatus(fundingId, OrderStatus.PAID, lastOrderId, chunkSize);
            if (paidOrderIds.isEmpty()) break;

            for (Long orderId : paidOrderIds) {
                orderService.publishSettlement(orderId);
            }
            lastOrderId = paidOrderIds.get(paidOrderIds.size() - 1);

            if (paidOrderIds.size() < chunkSize) break;
        }

        // 예약 주문 (RESERVED) → PAYING + 결제 요청 발행
        lastOrderId = 0L;
        while (true) {
            List<Long> reservedOrderIds = orderRepository.findOrderIdsByFundingIdAndStatus(fundingId, OrderStatus.RESERVED, lastOrderId, chunkSize);
            if (reservedOrderIds.isEmpty()) break;

            for (Long orderId : reservedOrderIds) {
                orderService.processPayingAndPublishSettlement(orderId);
            }
            lastOrderId = reservedOrderIds.get(reservedOrderIds.size() - 1);

            if (reservedOrderIds.size() < chunkSize) break;
        }

        log.info("[FUNDING_JUDGE] 펀딩 성공 주문 처리 완료 - fundingId: {}", fundingId);
    }

    public void handleFundingFailure(Long fundingId) {
        // TODO: Funding 실패 Notification Message 발행

        int chunkSize = 500;

        // 즉시 주문 (PAID) → CANCELLED + 환불 요청 발행
        Long lastOrderId = 0L;
        while (true) {
            List<Long> paidOrderIds = orderRepository.findOrderIdsByFundingIdAndStatus(fundingId, OrderStatus.PAID, lastOrderId, chunkSize);
            if (paidOrderIds.isEmpty()) break;

            for (Long orderId : paidOrderIds) {
                orderService.processCancelAndPublishRefund(orderId);
            }
            lastOrderId = paidOrderIds.get(paidOrderIds.size() - 1);

            if (paidOrderIds.size() < chunkSize) break;
        }

        // 예약 주문 (RESERVED) → CANCELLED 내부 처리
        lastOrderId = 0L;
        while (true) {
            List<Long> reservedOrderIds = orderRepository.findOrderIdsByFundingIdAndStatus(fundingId, OrderStatus.RESERVED, lastOrderId, chunkSize);
            if (reservedOrderIds.isEmpty()) break;

            for (Long orderId : reservedOrderIds) {
                orderService.processCancelledUpdate(orderId, CancelReason.FUNDING_FAILED.name());
            }
            lastOrderId = reservedOrderIds.get(reservedOrderIds.size() - 1);

            if (reservedOrderIds.size() < chunkSize) break;
        }

        log.info("[FUNDING_JUDGE] 펀딩 실패 주문 처리 완료 - fundingId: {}", fundingId);
    }


    // TODO: Funding 시작 Notification Message 발행
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
