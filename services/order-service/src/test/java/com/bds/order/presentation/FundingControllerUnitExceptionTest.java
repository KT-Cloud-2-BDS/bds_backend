package com.bds.order.presentation;

import com.bds.order.application.FundingService;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.global.exception.ErrorCode;
import com.bds.order.presentation.controller.FundingController;
import com.bds.order.presentation.dto.FundingCreateRequestDto;
import com.bds.support.MockMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FundingController.class)
class FundingControllerUnitExceptionTest extends MockMvcTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 7, 1, 12, 0);
    @MockitoBean
    private FundingService fundingService;

    @Value("${internal.gateway-secret}")
    private String gatewaySecret;

    @Nested
    @DisplayName("펀딩 상세 조회 예외")
    class GetFundingDetailExceptionTest {

        @Test
        void 존재하지_않는_펀딩이면_404를_반환한다() throws Exception {
            given(fundingService.getFundingDetail(999L))
                    .willThrow(new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

            mockMvc.perform(get("/api/fundings/999")
                            .header("X-User-Id", "1")
                            .header("X-Internal-Secret", gatewaySecret))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("펀딩 생성 예외")
    class CreateFundingExceptionTest {

        @Test
        void MAKER가_아니면_403을_반환한다() throws Exception {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, NOW, NOW.plusDays(30), NOW.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, NOW.plusDays(60), 3000L))
                    , null
            );

            given(fundingService.createFunding(eq(1L), eq(false), any()))
                    .willThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

            mockMvc.perform(post("/api/fundings")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", List.of("USER"))
                            .header("X-Internal-Secret", gatewaySecret)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void title이_빈_문자열이면_400을_반환한다() throws Exception {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "", 1000000L, NOW, NOW.plusDays(30), NOW.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, NOW.plusDays(60), 3000L))
                    , null
            );

            mockMvc.perform(post("/api/fundings")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", List.of("MAKER"))
                            .header("X-Internal-Secret", gatewaySecret)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void goalAmount가_0이면_400을_반환한다() throws Exception {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 0L, NOW, NOW.plusDays(30), NOW.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, NOW.plusDays(60), 3000L))
                    , null
            );

            mockMvc.perform(post("/api/fundings")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", List.of("MAKER"))
                            .header("X-Internal-Secret", gatewaySecret)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void startAt이_없으면_400을_반환한다() throws Exception {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, null, NOW.plusDays(30), NOW.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, NOW.plusDays(60), 3000L))
                    , null
            );

            mockMvc.perform(post("/api/fundings")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", List.of("MAKER"))
                            .header("X-Internal-Secret", gatewaySecret)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 리워드_name이_없으면_400을_반환한다() throws Exception {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, NOW, NOW.plusDays(30), NOW.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            null, "설명", 100, null, 10000L, NOW.plusDays(60), 3000L))
                    , null
            );

            mockMvc.perform(post("/api/fundings")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", List.of("MAKER"))
                            .header("X-Internal-Secret", gatewaySecret)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 리워드_price가_음수이면_400을_반환한다() throws Exception {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, NOW, NOW.plusDays(30), NOW.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, -1L, NOW.plusDays(60), 3000L))
                    , null
            );

            mockMvc.perform(post("/api/fundings")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", List.of("MAKER"))
                            .header("X-Internal-Secret", gatewaySecret)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
