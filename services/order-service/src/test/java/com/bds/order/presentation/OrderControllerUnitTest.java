package com.bds.order.presentation;

import com.bds.order.application.OrderService;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.domain.reward.BadgeType;
import com.bds.order.infrastructure.messaging.OrderEventPublisher;
import com.bds.order.presentation.controller.OrderController;
import com.bds.order.presentation.dto.*;
import com.bds.support.MockMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerUnitTest extends MockMvcTestSupport {

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @Nested
    @DisplayName("주문 목록 조회 API")
    class ListOrdersTest {

        @Test
        void 주문_목록을_정상_응답한다() throws Exception {
            LocalDateTime now = LocalDateTime.now();
            OrderResponseDto dto = new OrderResponseDto(
                    1L, "ORD-001", OrderStatus.PENDING, now,
                    "테스트 펀딩", 100L, false,
                    36000L, null, false
            );

            given(orderService.getAllOrders(eq(1L), any())).willReturn(List.of(dto));

            mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "1")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].orderNo").value("ORD-001"))
                    .andExpect(jsonPath("$[0].orderStatus").value("PENDING"))
                    .andExpect(jsonPath("$[0].billingAmount").value(36000))
                    .andExpect(jsonPath("$[0].title").value("테스트 펀딩"));
        }
    }

    @Nested
    @DisplayName("주문 상세 조회 API")
    class GetOrderDetailTest {

        @Test
        void 주문_상세를_정상_응답한다() throws Exception {
            LocalDateTime now = LocalDateTime.now();
            RewardItemDto rewardItem = new RewardItemDto(1L, 2, "리워드A", 20000L, BadgeType.ULTRA_EARLY_BIRD, 3000L);
            OrderDetailResponseDto dto = new OrderDetailResponseDto(
                    1L, "ORD-001", OrderStatus.PAID, now,
                    "테스트 펀딩", 100L, false,
                    now, false,
                    List.of(rewardItem),33000L, 3000L, 36000L, null
            );

            given(orderService.getOrderDetail(1L, 1L)).willReturn(dto);

            mockMvc.perform(get("/api/orders/1")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNo").value("ORD-001"))
                    .andExpect(jsonPath("$.orderStatus").value("PAID"))
                    .andExpect(jsonPath("$.rewards[0].name").value("리워드A"))
                    .andExpect(jsonPath("$.rewardAmount").value(33000))
                    .andExpect(jsonPath("$.totalBillingAmount").value(36000));
        }
    }

    @Nested
    @DisplayName("빌링 생성 API")
    class CreateBillingTest {

        @Test
        void 빌링을_정상_응답한다() throws Exception {
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 2)
            ));

            RewardItemDto rewardDto = new RewardItemDto(
                    1L, 2, "리워드A", 20000L, BadgeType.ULTRA_EARLY_BIRD, 3000L
            );
            BillingResponseDto responseDto = BillingResponseDto.from(Order.create(1L, 20000L, 3000L, OrderStatus.PENDING), List.of(rewardDto));

            given(orderService.createBilling(eq(1L), any())).willReturn(responseDto);

            mockMvc.perform(post("/api/orders/billing")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(1))
                    .andExpect(jsonPath("$.rewardAmount").value(20000))
                    .andExpect(jsonPath("$.totalShippingCharge").value(3000))
                    .andExpect(jsonPath("$.totalBillingAmount").value(23000))
                    .andExpect(jsonPath("$.rewards[0].name").value("리워드A"));
        }
    }

    @Nested
    @DisplayName("주문 취소 API")
    class CancelOrderTest {

        @Test
        void 주문_취소를_정상_응답한다() throws Exception {
            LocalDateTime cancelledAt = LocalDateTime.now();
            OrderCancelResponseDto dto = new OrderCancelResponseDto(
                    "ORD-001", OrderStatus.CANCELLED, cancelledAt, "REFUND_REQUESTED"
            );

            given(orderService.cancelOrder(1L, 1L)).willReturn(dto);

            mockMvc.perform(patch("/api/orders/1/cancel")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNo").value("ORD-001"))
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.refundStatus").value("REFUND_REQUESTED"));
        }
    }

    @Nested
    @DisplayName("주문 생성 API")
    class CreateOrderTest {

        @Test
        void 정상적으로_주문을_생성하면_200을_응답한다() throws Exception {
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(1L, 1L, true);

            OrderCreateResponseDto responseDto = new OrderCreateResponseDto(
                    1L, "ORD-001", 36000L, null, null);

            given(orderService.createOrder(eq(1L), any())).willReturn(responseDto);

            mockMvc.perform(post("/api/orders")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNo").value("ORD-001"))
                    .andExpect(jsonPath("$.totalBillingAmount").value(36000L));
        }
    }
}