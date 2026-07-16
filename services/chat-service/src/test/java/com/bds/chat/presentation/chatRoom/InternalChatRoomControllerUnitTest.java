package com.bds.chat.presentation.chatRoom;

import com.bds.chat.application.chatRoom.dto.ChatRoomResponseDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalChatRoomControllerUnitTest {

    @Mock ChatRoomService chatRoomService;

    @InjectMocks InternalChatRoomController controller;

    private MockMvc mockMvc;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("펀딩방 생성")
    class CreateFundingRoomTest {

        @Test
        void 펀딩방_생성이_201을_반환한다() throws Exception {
            given(chatRoomService.createFundingRoom(anyLong(), any()))
                    .willReturn(new ChatRoomResponseDto(10L, "FUNDING", 1L, List.of(), 2L, NOW, "ACTIVE"));

            mockMvc.perform(post("/internal/chat/fundings/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"creatorId\": 2}"))
                    .andExpect(status().isCreated());
        }
    }
}
