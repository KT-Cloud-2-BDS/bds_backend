package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.domain.orderReward.OrderReward;
import com.bds.order.fixture.OrderFixture;
import com.bds.order.infrastructure.orderReward.OrderRewardMapper;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderMapperUnitTest {

    @Mock
    private OrderRewardMapper orderRewardMapper;

    @Mock
    private RewardJpaRepository rewardJpaRepository;

    @InjectMocks
    private OrderMapper orderMapper;

    @Nested
    @DisplayName("JpaEntity에서 도메인으로 변환")
    class ToDomainTest {

        @Test
        void 정상적으로_도메인_객체로_변환한다() {
            OrderJpaEntity entity = OrderJpaEntity.builder()
                    .id(1L)
                    .orderNo("ORD-ABC123")
                    .memberId(100L)
                    .status(OrderStatus.PAID)
                    .totalRewardAmount(40000L)
                    .totalShippingCharge(3000L)
                    .cancelReason(null)
                    .cancelledAt(null)
                    .build();

            Order order = orderMapper.toDomain(entity);

            assertThat(order.getId()).isEqualTo(1L);
            assertThat(order.getOrderNo()).isEqualTo("ORD-ABC123");
            assertThat(order.getMemberId()).isEqualTo(100L);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(order.getTotalRewardAmount()).isEqualTo(40000L);
            assertThat(order.getTotalShippingCharge()).isEqualTo(3000L);
            assertThat(order.getCancelReason()).isNull();
            assertThat(order.getCancelledAt()).isNull();
        }

        @Test
        void 취소된_주문을_도메인으로_변환한다() {
            LocalDateTime cancelledAt = LocalDateTime.now().minusDays(1);
            OrderJpaEntity entity = OrderJpaEntity.builder()
                    .id(2L)
                    .orderNo("ORD-DEF456")
                    .memberId(200L)
                    .status(OrderStatus.CANCELLED)
                    .totalRewardAmount(50000L)
                    .totalShippingCharge(5000L)
                    .cancelReason(CancelReason.USER_CANCEL.name())
                    .cancelledAt(cancelledAt)
                    .build();

            Order order = orderMapper.toDomain(entity);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.USER_CANCEL.name());
            assertThat(order.getCancelledAt()).isEqualTo(cancelledAt);
        }
    }

    @Nested
    @DisplayName("도메인에서 JpaEntity로 변환")
    class ToJpaEntityTest {

        @Test
        void 정상적으로_JpaEntity로_변환한다() {
            Order order = OrderFixture.createOrder(100L, OrderStatus.PAID, 40000L, 3000L);

            OrderJpaEntity entity = orderMapper.toJpaEntity(order);

            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getMemberId()).isEqualTo(100L);
            assertThat(entity.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(entity.getTotalRewardAmount()).isEqualTo(40000L);
            assertThat(entity.getTotalShippingCharge()).isEqualTo(3000L);
            assertThat(entity.getCancelReason()).isNull();
            assertThat(entity.getCancelledAt()).isNull();
        }

        @Test
        void 취소된_주문을_JpaEntity로_변환한다() {
            LocalDateTime cancelledAt = LocalDateTime.now().minusDays(1);
            Order order = OrderFixture.createCancelOrder(cancelledAt);

            OrderJpaEntity entity = orderMapper.toJpaEntity(order);

            assertThat(entity.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(entity.getCancelReason()).isEqualTo(CancelReason.USER_CANCEL.name());
            assertThat(entity.getCancelledAt()).isEqualTo(cancelledAt);
        }

        @Test
        void OrderReward가_null인_주문을_JpaEntity로_변환한다() {
            Order order = Order.reconstitute(
                    1L, "ORD-001", 100L, OrderStatus.PENDING,
                    20000L, 3000L, null,
                    null, LocalDateTime.now(), LocalDateTime.now(), null, null
            );

            OrderJpaEntity entity = orderMapper.toJpaEntity(order);

            assertThat(entity.getOrderRewards()).isEmpty();
        }

        @Test
        void OrderReward가_빈_리스트인_주문을_JpaEntity로_변환한다() {
            Order order = Order.reconstitute(
                    1L, "ORD-001", 100L, OrderStatus.PENDING,
                    20000L, 3000L, List.of(),
                    null, LocalDateTime.now(), LocalDateTime.now(), null, null
            );

            OrderJpaEntity entity = orderMapper.toJpaEntity(order);

            assertThat(entity.getOrderRewards()).isEmpty();
        }

        @Test
        void OrderReward가_있는_주문을_JpaEntity로_변환한다() {
            OrderReward orderReward = OrderReward.reconstitute(1L, 1L, 10L, 2, 3000L, 20000L);
            Order order = Order.reconstitute(
                    1L, "ORD-001", 100L, OrderStatus.PAYING,
                    20000L, 3000L, List.of(orderReward),
                    null, LocalDateTime.now(), LocalDateTime.now(), null, null
            );

            RewardJpaEntity rewardRef = mock(RewardJpaEntity.class);
            given(rewardJpaRepository.getReferenceById(10L)).willReturn(rewardRef);

            OrderJpaEntity entity = orderMapper.toJpaEntity(order);

            assertThat(entity.getOrderRewards()).hasSize(1);
            verify(rewardJpaRepository).getReferenceById(10L);
        }
    }
}