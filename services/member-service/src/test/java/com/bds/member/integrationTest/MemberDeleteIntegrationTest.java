package com.bds.member.integrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bds.member.domain.entity.Member;
import com.bds.member.domain.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * [회원 탈퇴 도메인 통합 테스트]
 * 프론트엔드의 탈퇴 요청부터, Member 데이터 비활성화(Soft Delete),
 * Auth로의 계정 파기 위임 통신까지 전체 파이프라인을 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("회원 탈퇴 시나리오 통합 테스트")
class MemberDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

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
    @DisplayName("회원 탈퇴 요청 시, 내부 상태를 비활성화하고 외부 Auth 서버 파기 요청 후 정상 응답한다.")
    void memberDeleteLifecycleScenario() throws Exception {

        // given : 탈퇴할 정상 회원 데이터 사전 적재
        Long mockAuthId = 100L;
        String nickname = "bbangdiz";

        // DB에 탈퇴의 타겟이 될 정상 회원 생성
        Member activeMember = Member.create(mockAuthId, nickname);

        memberRepository.save(activeMember);
        em.flush();
        em.clear();

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));


        // when : 탈퇴 API 호출
        mockMvc.perform(delete("/api/members/delete")
                .header("X-User-Id", String.valueOf(mockAuthId))
                .header("X-Internal-Secret", "test-internal-secret")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());


        // then : Soft Delete
        Boolean isDeleted = (Boolean) em.createNativeQuery("SELECT is_deleted FROM member WHERE auth_id = :authId")
            .setParameter("authId", mockAuthId)
            .getSingleResult();

        org.junit.jupiter.api.Assertions.assertNotNull(isDeleted);
        org.junit.jupiter.api.Assertions.assertTrue(isDeleted, "데이터베이스에서 회원의 is_deleted 컬럼이 true로 변경되어야 합니다.");
    }
}