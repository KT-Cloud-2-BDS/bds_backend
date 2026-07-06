package com.bds.order.presentation;

import com.bds.order.application.OrderService;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.presentation.controller.OrderController;
import com.bds.order.presentation.dto.OrderResponseDto;
import com.bds.support.MockMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerUnitTest extends MockMvcTestSupport {

    @MockitoBean
    private OrderService orderService;

    @Nested
    @DisplayName("주문 목록 조회 정상 테스트")
    class ListOrdersTest {

        @Test
        void 인증된_사용자가_주문_목록을_조회한다() throws Exception {
            OrderResponseDto dto = new OrderResponseDto(
                    "ORD-ABC123DEF456",
                    OrderStatus.PENDING,
                    LocalDateTime.of(2025, 1, 15, 10, 0),
                    null,
                    null,
                    false,
                    33000L,
                    null,
                    null,
                    false
            );

            given(orderService.getAllOrders(eq(1L), any())).willReturn(List.of(dto));

            mockMvc.perform(get("/api/orders/")
                            .header("X-User-Id", "1")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].orderNo").value("ORD-ABC123DEF456"))
                    .andExpect(jsonPath("$[0].orderStatus").value("PENDING"))
                    .andExpect(jsonPath("$[0].billingAmount").value(33000));
        }
    }
}
