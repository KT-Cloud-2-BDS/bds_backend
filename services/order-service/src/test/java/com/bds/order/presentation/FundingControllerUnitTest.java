package com.bds.order.presentation;

import com.bds.order.application.FundingService;
import com.bds.order.presentation.controller.FundingController;
import com.bds.order.presentation.dto.FundingCreateRequestDto;
import com.bds.order.presentation.dto.FundingCreateResponseDto;
import com.bds.order.presentation.dto.FundingDetailResponseDto;
import com.bds.order.presentation.dto.FundingListResponseDto;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FundingController.class)
class FundingControllerUnitTest extends MockMvcTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 7, 1, 12, 0);
    @MockitoBean
    private FundingService fundingService;

    @Value("${internal.gateway-secret}")
    private String gatewaySecret;

    @Nested
    @DisplayName("펀딩 목록 조회 API")
    class ListFundingsTest {

        @Test
        void 전체_펀딩_목록을_정상_응답한다() throws Exception {
            FundingListResponseDto dto = new FundingListResponseDto(
                    1L, "테스트 펀딩", 100L, "ACTIVE",
                    1000000L, 500000L, 5,
                    NOW.minusDays(10), NOW.plusDays(30), false, NOW
            );

            given(fundingService.getFundings(null)).willReturn(List.of(dto));

            mockMvc.perform(get("/api/fundings")
                            .header("X-User-Id", "1")
                            .header("X-Internal-Secret", gatewaySecret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("테스트 펀딩"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].goalAmount").value(1000000));
        }

        @Test
        void status_파라미터로_필터링_요청한다() throws Exception {
            FundingListResponseDto dto = new FundingListResponseDto(
                    1L, "테스트 펀딩", 100L, "SCHEDULED",
                    1000000L, 0L, 0,
                    NOW.plusDays(1), NOW.plusDays(30), false, NOW
            );

            given(fundingService.getFundings("SCHEDULED")).willReturn(List.of(dto));

            mockMvc.perform(get("/api/fundings")
                            .header("X-User-Id", "1")
                            .header("X-Internal-Secret", gatewaySecret)
                            .param("status", "SCHEDULED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
        }
    }

    @Nested
    @DisplayName("펀딩 상세 조회 API")
    class GetFundingDetailTest {

        @Test
        void 펀딩_상세와_리워드를_정상_응답한다() throws Exception {
            FundingDetailResponseDto.RewardDto rewardDto = new FundingDetailResponseDto.RewardDto(
                    1L, "리워드A", "설명A", 100, 50,
                    "EARLY_BIRD", 10000L, NOW.plusDays(60), 3000L
            );
            FundingDetailResponseDto dto = new FundingDetailResponseDto(
                    1L, "테스트 펀딩", 100L, "ACTIVE",
                    1000000L, 500000L, 5,
                    NOW.minusDays(10), NOW.plusDays(30), NOW.plusDays(31),
                    false, NOW, NOW, List.of(rewardDto)
            );

            given(fundingService.getFundingDetail(1L)).willReturn(dto);

            mockMvc.perform(get("/api/fundings/1")
                            .header("X-User-Id", "1")
                            .header("X-Internal-Secret", gatewaySecret))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("테스트 펀딩"))
                    .andExpect(jsonPath("$.rewards[0].name").value("리워드A"))
                    .andExpect(jsonPath("$.rewards[0].badgeType").value("EARLY_BIRD"));
        }
    }

    @Nested
    @DisplayName("펀딩 생성 API")
    class CreateFundingTest {

        @Test
        void 펀딩을_정상_생성한다() throws Exception {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, NOW, NOW.plusDays(30), NOW.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, NOW.plusDays(60), 3000L))
                    , null
            );

            FundingCreateResponseDto response = new FundingCreateResponseDto(
                    1L, "새 펀딩", "SCHEDULED", "INSTANT", NOW, NOW.plusDays(30), NOW
            );

            given(fundingService.createFunding(eq(1L), eq(true), any())).willReturn(response);

            mockMvc.perform(post("/api/fundings")
                            .header("X-User-Id", "1")
                            .header("X-User-Roles", List.of("MAKER"))
                            .header("X-Internal-Secret", gatewaySecret)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fundingId").value(1))
                    .andExpect(jsonPath("$.title").value("새 펀딩"))
                    .andExpect(jsonPath("$.type").value("INSTANT"))
                    .andExpect(jsonPath("$.status").value("SCHEDULED"));
        }

    }
}
