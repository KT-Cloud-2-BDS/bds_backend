package com.bds.order.presentation;

import com.bds.order.application.OrderService;
import com.bds.order.presentation.controller.OrderController;
import com.bds.support.MockMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerExceptionTest extends MockMvcTestSupport {

    @MockitoBean
    private OrderService orderService;

    @Nested
    @DisplayName("주문 목록 조회 예외 테스트")
    class ListOrdersExceptionTest {

        @Test
        void 인증없이_요청하면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/api/orders/")).andExpect(status().isUnauthorized());
        }
    }
}