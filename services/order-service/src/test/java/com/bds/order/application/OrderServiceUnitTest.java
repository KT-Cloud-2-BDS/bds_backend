package com.bds.order.application;

import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.order.OrderListProjection;
import com.bds.order.presentation.dto.OrderResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("주문 목록 조회 정상 테스트")
    class GetAllOrdersTest {

        @Test
        void 회원의_주문_목록을_조회한다() {
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            OrderListProjection projection = new OrderListProjection(
                    1L,
                    "ORD-001",
                    OrderStatus.PENDING,
                    33000L,
                    3000L,
                    LocalDateTime.now().minusYears(1),
                    "Title",
                    100L,
                    LocalDateTime.now().minusDays(1),
                    false
            );

            given(orderRepository.findOrderListByMemberId(memberId, pageable)).willReturn(List.of(projection));

            List<OrderResponseDto> result = orderService.getAllOrders(memberId, pageable);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).orderNo()).isEqualTo("ORD-001");
            assertThat(result.get(0).orderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.get(0).billingAmount()).isEqualTo(33000L);
            assertThat(result.get(0).title()).isEqualTo("Title");
            assertThat(result.get(0).isEnded()).isTrue();
        }
    }
}