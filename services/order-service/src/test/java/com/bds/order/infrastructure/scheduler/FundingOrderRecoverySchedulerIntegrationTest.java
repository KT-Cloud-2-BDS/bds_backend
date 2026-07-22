package com.bds.order.infrastructure.scheduler;

import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.order.infrastructure.messaging.publisher.PaymentEventPublisher;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaRepository;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.order.presentation.dto.BillingRequestDto;
import com.bds.order.presentation.dto.BillingResponseDto;
import com.bds.order.presentation.dto.RewardQuantityDto;
import com.bds.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@Transactional
class FundingOrderRecoverySchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FundingOrderRecoveryScheduler recoveryScheduler;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FundingJpaRepository fundingJpaRepository;

    @Autowired
    private RewardJpaRepository rewardJpaRepository;

    @Autowired
    private OrderRewardJpaRepository orderRewardJpaRepository;

    @MockitoBean
    private PaymentEventPublisher paymentEventPublisher;

    private FundingJpaEntity savedFunding;
    private RewardJpaEntity savedReward;

    @AfterEach
    void tearDown() {
        orderRewardJpaRepository.deleteAll();
        orderRepository.deleteAll();
        rewardJpaRepository.deleteAll();
        fundingJpaRepository.deleteAll();
    }

    private void setUpFunding(FundingStatus status, FundingType type) {
        LocalDateTime now = LocalDateTime.now();
        savedFunding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "테스트 펀딩", 100L, status, type,
                now.minusDays(30), now.plusDays(2), now.plusDays(3),
                1, 500000L, 1000000L, status == FundingStatus.SUCCESS, new ArrayList<>()
        ));
        savedReward = rewardJpaRepository.save(new RewardJpaEntity(
                null, savedFunding, "리워드A", "설명A", 100, 10,
                null, 10000L, now.plusDays(60), 3000L
        ));
    }

    private Long createOrderWithStatus(OrderStatus targetStatus) {
        boolean isReserved = (targetStatus == OrderStatus.RESERVED);
        Long orderId = createBillingAndGetOrderId(isReserved);
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();

        if (targetStatus == OrderStatus.PAYING) {
            order.updateStatus(OrderStatus.PAYING);
        } else if (targetStatus == OrderStatus.PAID) {
            order.updateStatus(OrderStatus.PAYING);
            order.updateStatus(OrderStatus.PAID);
        } else if (targetStatus == OrderStatus.CANCELLED) {
            order.updateStatus(OrderStatus.PAYING);
            order.cancelOrder(CancelReason.FUNDING_FAILED.name());
        }

        return orderRepository.save(order).getId();
    }

    private Long createBillingAndGetOrderId(boolean isReserved) {
        BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), isReserved, List.of(
                new RewardQuantityDto(savedReward.getId(), 1)
        ));
        BillingResponseDto billing = orderService.createBilling(1L, reqDto);
        return billing.orderId();
    }

    @Nested
    @DisplayName("Reserved + Failed 복구")
    class RecoverReservedFailure {
        @Test
        void RESERVED_주문을_취소한다() {
            setUpFunding(FundingStatus.FAILED, FundingType.RESERVED);
            Long orderId = createOrderWithStatus(OrderStatus.RESERVED);

            recoveryScheduler.recover();

            Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Reserved + Success 복구")
    class RecoverReservedSuccess {
        @Test
        void PAYING_주문을_재발송하고_RESERVED_주문을_처리한다() {
            setUpFunding(FundingStatus.SUCCESS, FundingType.RESERVED);
            createOrderWithStatus(OrderStatus.PAYING);

            recoveryScheduler.recover();

            verify(paymentEventPublisher, atLeastOnce()).publishSettlement(argThat(event ->
                    event.type().equals("RESERVED_FUNDING_CONFIRMED") &&
                            !event.items().isEmpty()));
        }
    }

    @Nested
    @DisplayName("Instant + Success 복구")
    class RecoverInstantSuccess {
        @Test
        void PAID_주문에_정산_확정을_발행한다() {
            setUpFunding(FundingStatus.SUCCESS, FundingType.INSTANT);
            createOrderWithStatus(OrderStatus.PAID);

            recoveryScheduler.recover();

            verify(paymentEventPublisher, atLeastOnce()).publishSettlement(argThat(event ->
                    event.type().equals("SETTLEMENT_CONFIRMED") &&
                            event.creatorMemberId().equals(100L) &&
                            !event.items().isEmpty()));
        }
    }

    @Nested
    @DisplayName("Instant + Failed 복구")
    class RecoverInstantFailure {
        @Test
        void CANCELLED_주문을_재발송하고_PAID_주문을_처리한다() {
            setUpFunding(FundingStatus.FAILED, FundingType.INSTANT);
            createOrderWithStatus(OrderStatus.CANCELLED);
            createOrderWithStatus(OrderStatus.PAID);

            recoveryScheduler.recover();

            verify(paymentEventPublisher, atLeastOnce()).publishSettlement(argThat(event ->
                    event.type().equals("FUNDING_FAILED_REFUND") &&
                            !event.items().isEmpty()));
        }
    }

    @Nested
    @DisplayName("복구 대상 없음")
    class NoRecoveryTarget {
        @Test
        void 복구_대상_주문이_없어도_정상_완료된다(CapturedOutput output) {
            LocalDateTime now = LocalDateTime.now();
            fundingJpaRepository.save(new FundingJpaEntity(
                    null, "빈 펀딩", 100L, FundingStatus.SUCCESS, FundingType.INSTANT,
                    now.minusDays(30), now.minusDays(1), now.plusDays(1),
                    0, 500000L, 1000000L, true, new ArrayList<>()
            ));

            recoveryScheduler.recover();

            verifyNoInteractions(paymentEventPublisher);
            assertThat(output.getOut()).contains("[FUNDING_ORDER_RECOVERY] 배치 완료");
        }

        @Test
        @DisplayName("3일 이전 펀딩은 복구 대상에서 제외된다")
        void 펀딩이_3일_이전이면_복구_대상에서_제외된다(CapturedOutput output) {
            LocalDateTime now = LocalDateTime.now();
            FundingJpaEntity oldFunding = new FundingJpaEntity(
                    null, "오래된 펀딩", 100L, FundingStatus.SUCCESS, FundingType.INSTANT,
                    now.minusDays(30), now.minusDays(10), now.minusDays(5),
                    0, 500000L, 1000000L, true, new ArrayList<>()
            );
            fundingJpaRepository.save(oldFunding);

            recoveryScheduler.recover();

            verifyNoInteractions(paymentEventPublisher);
            assertThat(output.getOut()).contains("[FUNDING_ORDER_RECOVERY] 배치 완료");
        }
    }
}