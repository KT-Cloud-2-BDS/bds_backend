package com.bds.chat.presentation.message;

import com.bds.chat.application.message.dto.MessageDeleteResponseDto;
import com.bds.chat.application.message.dto.MessageListResponseDto;
import com.bds.chat.application.message.service.MessageService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerUnitTest {

    @Mock MessageService messageService;

    @InjectMocks MessageController controller;

    private MockMvc mockMvc;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MessageListResponseDto dummyMessageList() {
        return new MessageListResponseDto(List.of(), null, false, 0);
    }

    private MessageDeleteResponseDto dummyDeleteResponse() {
        return new MessageDeleteResponseDto(100L, true, NOW);
    }

    @Nested
    @DisplayName("채팅 이력 조회")
    class GetHistoryTest {

        @Test
        void 채팅_이력_조회가_200을_반환한다() throws Exception {
            given(messageService.getHistory(anyLong(), any())).willReturn(dummyMessageList());

            mockMvc.perform(get("/api/chat/rooms/messages")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("문의방 메시지 조회")
    class GetInquiryMessagesTest {

        @Test
        void 문의방_메시지_조회가_200을_반환한다() throws Exception {
            given(messageService.getInquiryMessages(anyLong(), anyLong(), any())).willReturn(dummyMessageList());

            mockMvc.perform(get("/api/chat/Inquiries/10/messages")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("펀딩방 메시지 조회")
    class GetFundingMessagesTest {

        @Test
        void 펀딩방_메시지_조회가_200을_반환한다() throws Exception {
            given(messageService.getFundingMessages(anyLong(), any())).willReturn(dummyMessageList());

            mockMvc.perform(get("/api/chat/fundings/10/messages")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteMessageTest {

        @Test
        void 메시지_삭제가_200을_반환한다() throws Exception {
            given(messageService.delete(anyLong(), anyLong())).willReturn(dummyDeleteResponse());

            mockMvc.perform(delete("/api/chat/messages/100")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isOk());
        }
    }
}
