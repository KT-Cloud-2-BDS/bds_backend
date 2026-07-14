package com.bds.order.infrastructure.scheduler;


import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
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

    @Transactional
    public void judgeFunding(Long fundingId) {
        Funding funding = fundingRepository.findByIdForUpdate(fundingId)
                .orElseThrow(() -> new IllegalStateException(
                        "[FUNDING_JUDGE] Funding not found: fundingId=" + fundingId));

        if (funding.getStatus() != FundingStatus.ACTIVE && funding.getStatus() != FundingStatus.HOLDING) {
            log.warn("[FUNDING_JUDGE] 판정 불가 상태: fundingId={}, status={}", fundingId, funding.getStatus());
            return;
        }

        boolean isSuccess = funding.getCurrentAmount() >= funding.getGoalAmount();

        if (isSuccess) {
            handleFundingSuccess(funding);
        } else {
            handleFundingFailure(funding);
        }
    }

    private void handleFundingSuccess(Funding funding) {
        funding.markSuccess();
        fundingRepository.save(funding);
        log.info("[FUNDING_JUDGE] 펀딩 성공 - fundingId: {}", funding.getId());
        // TODO: Funding 성공 Notification Message 발행

        // 즉시 주문 (PAID) → 정산 요청 발행
        List<Order> paidOrders = orderRepository.findByFundingIdAndStatus(funding.getId(), OrderStatus.PAID);
        for (Order order : paidOrders) {
            orderService.publishSettlement(order.getId());
        }
        log.info("[FUNDING_JUDGE] 즉시 주문 정산 요청 - {}건", paidOrders.size());

        // 예약 주문 (RESERVED) → PAYING + 결제 요청 발행
        List<Order> reservedOrders = orderRepository.findByFundingIdAndStatus(funding.getId(), OrderStatus.RESERVED);
        for (Order order : reservedOrders) {
            orderService.processPayingAndPublishSettlement(order.getId());
        }
        log.info("[FUNDING_JUDGE] 예약 주문 결제 요청 - {}건", reservedOrders.size());
    }

    private void handleFundingFailure(Funding funding) {
        funding.markFailed();
        fundingRepository.save(funding);
        log.info("[FUNDING_JUDGE] 펀딩 실패 - fundingId: {}", funding.getId());
        // TODO: Funding 실패 Notification Message 발행

        // 즉시 주문 (PAID) → CANCELLED + 환불 요청 발행
        List<Order> paidOrders = orderRepository.findByFundingIdAndStatus(funding.getId(), OrderStatus.PAID);
        for (Order order : paidOrders) {
            orderService.processCancelAndPublishRefund(order.getId());
        }
        log.info("[FUNDING_JUDGE] 즉시 주문 환불 요청 - {}건", paidOrders.size());

        // 예약 주문 (RESERVED) → CANCELLED 내부 처리 (결제 안 했으니 Pay 요청 없음)
        List<Order> reservedOrders = orderRepository.findByFundingIdAndStatus(funding.getId(), OrderStatus.RESERVED);
        for (Order order : reservedOrders) {
            orderService.processCancelledUpdate(order.getId(), CancelReason.FUNDING_FAILED.name());
        }
        log.info("[FUNDING_JUDGE] 예약 주문 취소 - {}건", reservedOrders.size());
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
