package com.bds.order.infrastructure.order;


import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceAdapterUnitTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderPersistenceAdapter orderPersistenceAdapter;

    @Nested
    @DisplayName("주문 저장")
    class SaveTest {

        @Test
        void 주문을_저장하고_도메인_객체를_반환한다() {
            Order order = Order.create(1L, 33000L, 3000L, OrderStatus.PENDING);
            OrderJpaEntity entity = OrderJpaEntity.builder()
                    .memberId(1L)
                    .status(OrderStatus.PENDING)
                    .totalRewardAmount(33000L)
                    .totalShippingCharge(3000L)
                    .build();
            OrderJpaEntity savedEntity = OrderJpaEntity.builder()
                    .id(1L)
                    .orderNo("ORD-ABC123")
                    .memberId(1L)
                    .status(OrderStatus.PENDING)
                    .totalRewardAmount(33000L)
                    .totalShippingCharge(3000L)
                    .build();
            Order savedOrder = Order.reconstitute(1L, "ORD-ABC123", 1L, OrderStatus.PENDING,
                    33000L, 3000L, List.of(), null, LocalDateTime.now(), LocalDateTime.now(), null);

            given(orderMapper.toJpaEntity(order)).willReturn(entity);
            given(orderJpaRepository.save(entity)).willReturn(savedEntity);
            given(orderMapper.toDomain(savedEntity)).willReturn(savedOrder);

            Order result = orderPersistenceAdapter.save(order);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getOrderNo()).isEqualTo("ORD-ABC123");
        }
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class FindOrderListTest {

        @Test
        void 회원의_주문_목록을_반환한다() {
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            LocalDateTime now = LocalDateTime.now();

            OrderListProjection projection = new OrderListProjection(
                    1L, "ORD-001", OrderStatus.PENDING,
                    33000L, 3000L, now,
                    "테스트 펀딩", 100L, now.plusDays(30), false
            );
            Page<OrderListProjection> page = new PageImpl<>(List.of(projection));

            given(orderJpaRepository.findOrderListWithFunding(memberId, pageable)).willReturn(page);

            List<OrderListProjection> result = orderPersistenceAdapter.findOrderListWithFunding(memberId, pageable);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).orderNo()).isEqualTo("ORD-001");
            assertThat(result.get(0).fundingTitle()).isEqualTo("테스트 펀딩");
        }
    }

    @Nested
    @DisplayName("주문 상세 조회")
    class FindOrderDetailTest {

        @Test
        void 존재하는_주문_상세를_반환한다() {
            Long memberId = 1L;
            Long orderId = 1L;
            LocalDateTime now = LocalDateTime.now();

            OrderDetailProjection projection = new OrderDetailProjection(
                    1L, "ORD-001", OrderStatus.PAID,
                    33000L, 3000L, now,
                    "테스트 펀딩", 100L, now.plusDays(30), false,
                    null, null
            );

            given(orderJpaRepository.findOrderWithFunding(memberId, orderId))
                    .willReturn(Optional.of(projection));

            Optional<OrderDetailProjection> result = orderPersistenceAdapter.findOrderDetailWithFunding(memberId, orderId);

            assertThat(result).isPresent();
            assertThat(result.get().orderNo()).isEqualTo("ORD-001");
        }

        @Test
        void 존재하지_않는_주문이면_빈값을_반환한다() {
            given(orderJpaRepository.findOrderWithFunding(1L, 999L))
                    .willReturn(Optional.empty());

            Optional<OrderDetailProjection> result = orderPersistenceAdapter.findOrderDetailWithFunding(1L, 999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("비관적 락으로 주문 조회")
    class FindByIdForUpdateTest {

        @Test
        void 존재하는_주문을_락과_함께_조회한다() {
            OrderJpaEntity entity = OrderJpaEntity.builder()
                    .id(1L)
                    .orderNo("ORD-001")
                    .memberId(1L)
                    .status(OrderStatus.PAYING)
                    .totalRewardAmount(33000L)
                    .totalShippingCharge(3000L)
                    .build();
            Order order = Order.reconstitute(1L, "ORD-001", 1L, OrderStatus.PAYING,
                    33000L, 3000L, List.of(), null, LocalDateTime.now(), LocalDateTime.now(), null);

            given(orderJpaRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));
            given(orderMapper.toDomain(entity)).willReturn(order);

            Optional<Order> result = orderPersistenceAdapter.findByIdForUpdate(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(OrderStatus.PAYING);
        }

        @Test
        void 존재하지_않는_주문이면_빈값을_반환한다() {
            given(orderJpaRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

            Optional<Order> result = orderPersistenceAdapter.findByIdForUpdate(999L);

            assertThat(result).isEmpty();
        }
    }
}