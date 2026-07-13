package com.bds.chat.presentation.blackList;

import com.bds.chat.application.blackList.service.BlackListService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BlackListControllerUnitExceptionTest {

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

    @Nested
    @DisplayName("인증 예외")
    class AuthExceptionTest {

        @Test
        void X_User_Id_헤더가_없으면_401을_반환한다() throws Exception {
            mockMvc.perform(post("/api/chat/fundings/10/ban")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetId\": 7}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("서비스 예외 처리")
    class ServiceExceptionTest {

        @Test
        void 서비스가_CONFLICT를_던지면_409를_반환한다() throws Exception {
            given(blackListService.create(anyLong(), anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.CONFLICT, "Already banned"));

            mockMvc.perform(post("/api/chat/fundings/10/ban")
                            .header("X-User-Id", "2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetId\": 7}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void 서비스가_FORBIDDEN을_던지면_403을_반환한다() throws Exception {
            given(blackListService.create(anyLong(), anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.FORBIDDEN, "Only creator can ban"));

            mockMvc.perform(post("/api/chat/fundings/10/ban")
                            .header("X-User-Id", "5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetId\": 7}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void 서비스가_NOT_FOUND를_던지면_404를_반환한다() throws Exception {
            given(blackListService.delete(anyLong(), anyLong(), anyLong()))
                    .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "No active ban"));

            mockMvc.perform(delete("/api/chat/fundings/10/ban/7")
                            .header("X-User-Id", "2"))
                    .andExpect(status().isNotFound());
        }
    }
}
