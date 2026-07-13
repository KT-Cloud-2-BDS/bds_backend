package com.bds.chat.presentation.chatRoom;

import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalChatRoomControllerUnitExceptionTest {

    @Mock ChatRoomService chatRoomService;

    @InjectMocks InternalChatRoomController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("펀딩방 생성 예외")
    class CreateFundingRoomExceptionTest {

        @Test
        void 서비스가_CONFLICT를_던지면_409를_반환한다() throws Exception {
            given(chatRoomService.createFundingRoom(anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.CONFLICT, "Funding room already exists"));

            mockMvc.perform(post("/internal/chat/fundings/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"creatorId\": 2}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void 서비스가_NOT_FOUND를_던지면_404를_반환한다() throws Exception {
            given(chatRoomService.createFundingRoom(anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "productId=1"));

            mockMvc.perform(post("/internal/chat/fundings/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"creatorId\": 2}"))
                    .andExpect(status().isNotFound());
        }
    }
}
