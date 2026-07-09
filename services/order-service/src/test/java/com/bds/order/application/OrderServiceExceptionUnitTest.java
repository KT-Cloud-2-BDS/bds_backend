package com.bds.order.application;


import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.domain.reward.Reward;
import com.bds.order.domain.reward.RewardRepository;
import com.bds.order.fixture.OrderFixture;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.presentation.dto.BillingRequestDto;
import com.bds.order.presentation.dto.RewardQuantityDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceExceptionUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private RewardRepository rewardRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("주문 상세 조회 예외")
    class GetOrderDetailExceptionTest {

        @Test
        void 존재하지_않는_주문이면_예외를_던진다() {
            given(orderRepository.findOrderDetailWithFunding(1L, 999L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("빌링 생성 예외")
    class CreateBillingExceptionTest {

        @Test
        void 펀딩이_존재하지_않으면_예외를_던진다() {
            BillingRequestDto reqDto = new BillingRequestDto(999L, false, List.of(
                    new RewardQuantityDto(1L, 1)
            ));

            given(fundingRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 펀딩_기간이_아니면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 1)
            ));

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE,
                    now.minusDays(30), now.minusDays(1), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 리워드가_존재하지_않으면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 1),
                    new RewardQuantityDto(2L, 1)
            ));

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    null, 10000L, now.plusDays(60), 3000L);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(any(), eq(1L)))
                    .willReturn(List.of(reward));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 동일한_리워드를_중복_선택하면_예외를_던진다() {
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 1),
                    new RewardQuantityDto(1L, 2)
            ));

            LocalDateTime now = LocalDateTime.now();
            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 리워드_재고가_부족하면_예외를_던진다() {
            LocalDateTime now = LocalDateTime.now();
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 100)
            ));

            Funding funding = Funding.of(1L, "펀딩", 100L, FundingStatus.ACTIVE,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 500000L, false, now, now);

            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 3,
                    null, 10000L, now.plusDays(60), 3000L);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(rewardRepository.findAllByIdAndFundingId(any(), eq(1L)))
                    .willReturn(List.of(reward));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소 예외")
    class CancelOrderExceptionTest {

        @Test
        void 존재하지_않는_주문이면_예외를_던진다() {
            given(orderRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 본인의_주문이_아니면_예외를_던진다() {
            Order order = OrderFixture.createOrder(2L, OrderStatus.PAID);

            given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 취소_불가_상태이면_예외를_던진다() {
            Order order = OrderFixture.createOrder(1L, OrderStatus.PENDING);

            given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
