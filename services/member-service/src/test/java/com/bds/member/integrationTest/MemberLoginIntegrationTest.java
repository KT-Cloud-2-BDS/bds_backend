package com.bds.member.integrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * [로그인 도메인 통합 테스트]
 * 로그인 요청 수신부터 Auth와의 통신 위임,
 * 발급받은 토큰을 클라이언트에게 반환하는 전체 파이프라인을 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("로그인 시나리오 통합 테스트")
class MemberLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static MockWebServer mockWebServer;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8081);
    }

    @AfterAll
    static void stopServer() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        mockWebServer.setDispatcher(new okhttp3.mockwebserver.QueueDispatcher());
    }

    @Test
    @DisplayName("프론트로부터 로그인 요청을 받으면, 가짜 Auth 서버 통신 후 토큰을 정상 반환한다.")
    void memberLoginLifecycleScenario() throws Exception {

        // given : 테스트 데이터 및 외부 가짜 응답 큐
        String email = "yeojin@email.com";
        String password = "password123!";

        // 가짜 Auth 서버가 발급해줄 목업(Mock) 토큰들
        String mockAccessToken = "mock.jwt.access.token.header.payload.signature";
        String mockRefreshToken = "mock.jwt.refresh.token.header.payload.signature";

        // Auth 서버가 정상적으로 비밀번호를 검증하고 토큰을 반환한다고 가정하여 응답 장전
        Map<String, String> mockAuthResponse = new HashMap<>();
        mockAuthResponse.put("accessToken", mockAccessToken);
        mockAuthResponse.put("refreshToken", mockRefreshToken);

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(objectMapper.writeValueAsString(mockAuthResponse)));

        // 프론트엔드에서 Member 서버로 보낼 로그인 요청 JSON 생성
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", email);
        loginRequest.put("password", password);

        String jsonContent = objectMapper.writeValueAsString(loginRequest);


        // when&then : 컨트롤러 요청 및 응답 검증
        // 로그인은 DB 상태 변화(INSERT)가 목표가 아니라, 인증 토큰 반환이 목표
        // MockMvc의 jsonPath를 사용하여 응답 본문 검증
        mockMvc.perform(post("/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(mockAccessToken))   // JSON 응답의 accessToken 필드 값 검증
            .andExpect(jsonPath("$.refreshToken").value(mockRefreshToken)); // JSON 응답의 refreshToken 필드 값 검증
    }
}