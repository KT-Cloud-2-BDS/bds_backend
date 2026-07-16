package com.bds.chat.presentation.blackList;

import com.bds.chat.application.blackList.dto.BlackListReponseDto;
import com.bds.chat.application.blackList.service.BlackListService;
import com.bds.chat.presentation.GlobalExceptionHandler;
import com.bds.common.resolver.LoginUserArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BlackListControllerUnitTest {

    @Mock BlackListService blackListService;

    @InjectMocks BlackListController controller;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private BlackListReponseDto dummyBanResponse(String status) {
        return new BlackListReponseDto(10L, 7L, status);
    }

    @Nested
    @DisplayName("블랙리스트 추가")
    class BanTest {

        @Test
        void 차단_요청이_201을_반환한다() throws Exception {
            given(blackListService.create(anyLong(), anyLong(), any()))
                    .willReturn(dummyBanResponse("ACTIVE"));

            mockMvc.perform(post("/api/chat/fundings/10/ban")
                            .header("X-User-Id", "2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetId\": 7, \"reason\": \"spam\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("블랙리스트 해제")
    class ReleaseBanTest {

        @Test
        void 차단_해제_요청이_200을_반환한다() throws Exception {
            given(blackListService.delete(anyLong(), anyLong(), anyLong()))
                    .willReturn(dummyBanResponse("RELEASED"));

            mockMvc.perform(delete("/api/chat/fundings/10/ban/7")
                            .header("X-User-Id", "2"))
                    .andExpect(status().isOk());
        }
    }
}
