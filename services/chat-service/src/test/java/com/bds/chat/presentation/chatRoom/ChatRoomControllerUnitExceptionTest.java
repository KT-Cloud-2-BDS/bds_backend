package com.bds.chat.presentation.chatRoom;

import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.member.service.InquiryRoomMemberService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatRoomControllerUnitExceptionTest {

    private static final String PSQL_UNIQUE_VIOLATION = "23505";

    @Mock ChatRoomService chatRoomService;
    @Mock InquiryRoomMemberService inquiryRoomMemberService;

    @InjectMocks ChatRoomController controller;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("인증 예외")
    class AuthExceptionTest {

        @Test
        void X_User_Id_헤더가_없으면_401을_반환한다() throws Exception {
            mockMvc.perform(post("/api/chat/Inquiries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\": 1}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("서비스 예외 처리")
    class ServiceExceptionTest {

        @Test
        void 서비스가_NOT_FOUND를_던지면_404를_반환한다() throws Exception {
            given(chatRoomService.getInquiryChatRoomById(anyLong(), anyLong()))
                    .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "roomId=10"));

            mockMvc.perform(get("/api/chat/Inquiries/10")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void 서비스가_FORBIDDEN을_던지면_403을_반환한다() throws Exception {
            given(chatRoomService.delete(anyLong(), anyLong()))
                    .willThrow(new BusinessException(ErrorCode.FORBIDDEN, "Only creator can close room"));

            mockMvc.perform(delete("/api/chat/rooms/10/close")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void 서비스가_CONFLICT를_던지면_409를_반환한다() throws Exception {
            given(chatRoomService.createInquiryRoom(anyLong(), anyLong()))
                    .willThrow(new BusinessException(ErrorCode.CONFLICT, "already exists"));

            mockMvc.perform(post("/api/chat/Inquiries")
                            .header("X-User-Id", "5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\": 1}"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("데이터 무결성 예외 처리")
    class DataIntegrityExceptionTest {

        @Test
        void 유니크_제약_위반_시_409를_반환한다() throws Exception {
            SQLException sqlEx = new SQLException("unique constraint", PSQL_UNIQUE_VIOLATION);
            given(chatRoomService.createInquiryRoom(anyLong(), anyLong()))
                    .willThrow(new DataIntegrityViolationException("constraint violation", sqlEx));

            mockMvc.perform(post("/api/chat/Inquiries")
                            .header("X-User-Id", "5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\": 1}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void 기타_데이터_무결성_오류_시_500을_반환한다() throws Exception {
            given(chatRoomService.createInquiryRoom(anyLong(), anyLong()))
                    .willThrow(new DataIntegrityViolationException("other integrity error"));

            mockMvc.perform(post("/api/chat/Inquiries")
                            .header("X-User-Id", "5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\": 1}"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("요청 형식 예외 처리")
    class RequestFormatExceptionTest {

        @Test
        void 잘못된_JSON_본문으로_요청하면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/chat/Inquiries")
                            .header("X-User-Id", "5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("not-valid-json"))
                    .andExpect(status().isBadRequest());
        }
    }
}
