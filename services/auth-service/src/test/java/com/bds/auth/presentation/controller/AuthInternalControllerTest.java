package com.bds.auth.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bds.auth.application.AuthService;
import com.bds.auth.presentation.dto.AuthCreateRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthInternalController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthInternalController 단위 테스트")
class AuthInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /internal/auths/account는 생성된 계정의 authId를 반환한다")
    void 계정생성_성공() throws Exception {
        // given
        given(authService.createAccount("yeojin@email.com", "password123!")).willReturn(1L);

        String requestBody = objectMapper.writeValueAsString(
            new AuthCreateRequestDto("yeojin@email.com", "password123!")
        );

        // when & then
        mockMvc.perform(post("/internal/auths/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verify(authService).createAccount("yeojin@email.com", "password123!");
    }

    @Test
    @DisplayName("DELETE /internal/auths/{authId}는 계정을 삭제하고 200을 반환한다")
    void 계정삭제_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/internal/auths/{authId}", 1L))
            .andExpect(status().isOk());

        verify(authService).deleteAuth(1L);
    }
}
