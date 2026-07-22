package com.bds.order.presentation;

import com.bds.order.application.OrderService;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.global.exception.ErrorCode;
import com.bds.order.presentation.controller.OrderController;
import com.bds.order.presentation.dto.BillingRequestDto;
import com.bds.order.presentation.dto.OrderCancelRequestDto;
import com.bds.order.presentation.dto.OrderCreateRequestDto;
import com.bds.order.presentation.dto.RewardQuantityDto;
import com.bds.support.MockMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerUnitExceptionTest extends MockMvcTestSupport {

    @MockitoBean
    private OrderService orderService;

    @Nested
    @DisplayName("인증 예외 응답")
    class AuthExceptionTest {

        @Test
        void 헤더에_유저정보가_없으면_401을_응답한다() throws Exception {
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 유저ID가_숫자가_아니면_401을_응답한다() throws Exception {
            mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "invalid"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("주문 조회 예외 응답")
    class GetOrderListExceptionTest {
        @Test
        void page가_음수이면_400을_응답한다() throws Exception {
            mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "1")
                            .param("page", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
    }

    @Nested
    @DisplayName("주문 상세 조회 예외 응답")
    class GetOrderDetailExceptionTest {

        @Test
        void 존재하지_않는_주문이면_404를_응답한다() throws Exception {
            given(orderService.getOrderDetail(1L, 999L))
                    .willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

            mockMvc.perform(get("/api/orders/999")
                            .header("X-User-Id", "1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("빌링 생성 예외 응답")
    class CreateBillingExceptionTest {

        @Test
        void 펀딩이_존재하지_않으면_404를_응답한다() throws Exception {
            BillingRequestDto reqDto = new BillingRequestDto(999L, false, List.of(
                    new RewardQuantityDto(1L, 1)
            ));

            given(orderService.createBilling(eq(1L), any()))
                    .willThrow(new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

            mockMvc.perform(post("/api/orders/billing")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("FUNDING_NOT_FOUND"));
        }

        @Test
        void 펀딩_기간이_아니면_403을_응답한다() throws Exception {
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 1)
            ));

            given(orderService.createBilling(eq(1L), any()))
                    .willThrow(new BusinessException(ErrorCode.FUNDING_NOT_AVAILABLE));

            mockMvc.perform(post("/api/orders/billing")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FUNDING_NOT_AVAILABLE"));
        }

        @Test
        void 동일한_리워드를_중복_선택하면_400을_응답한다() throws Exception {
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 1),
                    new RewardQuantityDto(1L, 2)
            ));

            given(orderService.createBilling(eq(1L), any()))
                    .willThrow(new BusinessException(ErrorCode.REWARD_DUPLICATED));

            mockMvc.perform(post("/api/orders/billing")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("REWARD_DUPLICATED"));
        }

        @Test
        void 리워드_재고가_부족하면_409를_응답한다() throws Exception {
            BillingRequestDto reqDto = new BillingRequestDto(1L, false, List.of(
                    new RewardQuantityDto(1L, 100)
            ));

            given(orderService.createBilling(eq(1L), any()))
                    .willThrow(new BusinessException(ErrorCode.REWARD_STOCK_INSUFFICIENT));

            mockMvc.perform(post("/api/orders/billing")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("REWARD_STOCK_INSUFFICIENT"));
        }

        @Test
        void 요청_바디가_유효하지_않으면_400을_응답한다() throws Exception {
            String invalidBody = """
                    {
                        "fundingId": null,
                        "rewards": []
                    }
                    """;

            mockMvc.perform(post("/api/orders/billing")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("주문 취소 예외 응답")
    class CancelOrderExceptionTest {

        @Test
        void 존재하지_않는_주문이면_404를_응답한다() throws Exception {
            OrderCancelRequestDto reqDto = new OrderCancelRequestDto(1L);
            given(orderService.cancelOrder(eq(1L), eq(999L), any(OrderCancelRequestDto.class)))
                    .willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

            mockMvc.perform(patch("/api/orders/999/cancel")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fundingId\":1}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        }

        @Test
        void 본인의_주문이_아니면_403을_응답한다() throws Exception {
            given(orderService.cancelOrder(eq(1L), eq(1L), any(OrderCancelRequestDto.class)))
                    .willThrow(new BusinessException(ErrorCode.ORDER_ACCESS_DENIED));

            mockMvc.perform(patch("/api/orders/1/cancel")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fundingId\":1}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));
        }

        @Test
        void 취소_불가_상태이면_400을_응답한다() throws Exception {
            given(orderService.cancelOrder(eq(1L), eq(1L), any(OrderCancelRequestDto.class)))
                    .willThrow(new BusinessException(ErrorCode.ORDER_STATUS_CHANGE_NOT_ALLOWED));

            mockMvc.perform(patch("/api/orders/1/cancel")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fundingId\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ORDER_STATUS_CHANGE_NOT_ALLOWED"));
        }
    }

    @Nested
    @DisplayName("주문 생성 예외 응답")
    class CreateOrderExceptionTest {

        @Test
        void 주문이_존재하지_않으면_404를_응답한다() throws Exception {
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(999L, 1L, true);

            given(orderService.createOrder(eq(1L), any()))
                    .willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

            mockMvc.perform(post("/api/orders")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        }

        @Test
        void 본인의_주문이_아니면_403을_응답한다() throws Exception {
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(1L, 1L, true);

            given(orderService.createOrder(eq(1L), any()))
                    .willThrow(new BusinessException(ErrorCode.ORDER_ACCESS_DENIED));

            mockMvc.perform(post("/api/orders")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));
        }

        @Test
        void 상태_변경이_불가하면_400을_응답한다() throws Exception {
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(1L, 1L, true);

            given(orderService.createOrder(eq(1L), any()))
                    .willThrow(new BusinessException(ErrorCode.ORDER_STATUS_CHANGE_NOT_ALLOWED));

            mockMvc.perform(post("/api/orders")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ORDER_STATUS_CHANGE_NOT_ALLOWED"));
        }

        @Test
        void 재고가_부족하면_409를_응답한다() throws Exception {
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(1L, 1L, true);

            given(orderService.createOrder(eq(1L), any()))
                    .willThrow(new BusinessException(ErrorCode.REWARD_STOCK_INSUFFICIENT));

            mockMvc.perform(post("/api/orders")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("REWARD_STOCK_INSUFFICIENT"));
        }
    }
}