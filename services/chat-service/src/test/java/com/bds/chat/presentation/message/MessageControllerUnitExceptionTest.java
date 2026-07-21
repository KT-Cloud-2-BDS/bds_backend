package com.bds.chat.presentation.message;

import com.bds.chat.application.message.service.MessageService;
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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerUnitExceptionTest {

    @Mock MessageService messageService;

    @InjectMocks MessageController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Nested
    @DisplayName("인증 예외")
    class AuthExceptionTest {

        @Test
        void X_User_Id_헤더가_없으면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/api/chat/rooms/messages"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("서비스 예외 처리")
    class ServiceExceptionTest {

        @Test
        void 서비스가_NOT_FOUND를_던지면_404를_반환한다() throws Exception {
            given(messageService.getInquiryMessages(anyLong(), anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "roomId=10"));

            mockMvc.perform(get("/api/chat/Inquiries/10/messages")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void 서비스가_FORBIDDEN을_던지면_403을_반환한다() throws Exception {
            given(messageService.delete(anyLong(), anyLong()))
                    .willThrow(new BusinessException(ErrorCode.FORBIDDEN, "Cannot delete"));

            mockMvc.perform(delete("/api/chat/messages/100")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isForbidden());
        }
    }
}
