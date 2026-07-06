package com.bds.order.application;

import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.presentation.dto.OrderResponseDto;
import com.bds.support.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.save(Order.create(1L, 33000L, OrderStatus.PENDING));
        orderRepository.save(Order.create(1L, 53000L, OrderStatus.PAID));
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
    }

    @Nested
    @DisplayName("주문 목록 조회 정상 테스트")
    class GetAllOrdersTest {

        @Test
        void 회원의_주문_목록을_DB에서_조회한다() {
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            List<OrderResponseDto> result = orderService.getAllOrders(memberId, pageable);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(OrderResponseDto::orderStatus)
                    .containsExactlyInAnyOrder(OrderStatus.PENDING, OrderStatus.PAID);
        }
    }
}