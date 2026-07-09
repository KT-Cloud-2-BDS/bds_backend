package com.bds.member.integrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bds.member.domain.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
 * [회원가입 도메인 통합 테스트]
 * 프론트엔드의 요청 수신부터, Auth와의 통신,
 * 최종 데이터베이스 적재까지의 전체 파이프라인을 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("회원가입 시나리오 통합 테스트")
public class MemberSignUpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private com.bds.member.infrastructure.persistence.adapter.MemberAdapter memberAdapter;

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
    @DisplayName("프론트로부터 회원가입 요청을 받으면, 가짜 Auth 서버 통신을 거쳐 최종 DB에 적재된다.")
    void memberSignUpLifecycleScenario() throws Exception {

        // given: 테스트 데이터 및 외부 가짜 응답 큐
        String email = "yeojin@email.com";
        String password = "password123!";
        String nickname = "BBandiz";
        Long mockAuthId = 100L;

        // Member 서버가 Auth 서버를 호출할 때, 무조건 상태코드 200과 mockAuthId를 반환하도록 세팅
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(objectMapper.writeValueAsString(mockAuthId)));

        Map<String, String> signUpRequest = new HashMap<>();
        signUpRequest.put("email", email);
        signUpRequest.put("password", password);
        signUpRequest.put("nickname", nickname);

        String jsonContent = objectMapper.writeValueAsString(signUpRequest);

        // when : MockMvc로 컨트롤러에 요청
        // DispatcherServlet을 거쳐 실제 컨트롤러와 서비스 로직이 정상적으로 연동되는지 검증
        mockMvc.perform(post("/api/member/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
            .andDo(print())
            .andExpect(status().isCreated());

        // then : 데이터 적재 확인
        // MemberAdapter를 통해 순수 도메인 member 검증
        Member savedMember = memberAdapter.findByAuthId(mockAuthId)
            .orElseThrow(() -> new IllegalArgumentException("회원이 저장되지 않았습니다!"));

        org.junit.jupiter.api.Assertions.assertNotNull(savedMember);
        org.junit.jupiter.api.Assertions.assertEquals(nickname, savedMember.getNickname());
        org.junit.jupiter.api.Assertions.assertEquals(mockAuthId, savedMember.getAuthId());
    }
}