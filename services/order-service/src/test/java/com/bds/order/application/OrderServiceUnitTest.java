package com.bds.order.application;

import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
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

            Order order = Order.create(memberId, 33000L, OrderStatus.PENDING);

            given(orderRepository.findAllByMemberId(memberId, pageable)).willReturn(List.of(order));

            List<OrderResponseDto> result = orderService.getAllOrders(memberId, pageable);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).orderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.get(0).billingAmount()).isEqualTo(33000L);
        }
    }
}