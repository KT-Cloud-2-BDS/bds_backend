package com.bds.member.integrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bds.member.domain.entity.Member;
import com.bds.member.domain.repository.MemberRepository;
import jakarta.persistence.EntityManager;
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
 * [회원 정보 조회 도메인 통합 테스트]
 * 로그인된 유저 헤더로 회원 정보 조회 요청 시,
 * 저장된 닉네임이 정상 응답되는지 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("회원 정보 조회 시나리오 통합 테스트")
class MemberInfoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("가입된 회원이 정보 조회를 요청하면 닉네임을 담아 200으로 응답한다.")
    void memberInfoLifecycleScenario() throws Exception {

        // given : 조회 대상이 될 정상 회원 데이터 사전 적재
        Long mockAuthId = 100L;
        String nickname = "bbangdiz";

        Member activeMember = Member.create(mockAuthId, nickname);
        memberRepository.save(activeMember);
        em.flush();
        em.clear();

        // when & then : 정보 조회 API 호출
        mockMvc.perform(get("/api/members/info")
                .header("X-User-Id", String.valueOf(mockAuthId))
                .header("X-Internal-Secret", "test-internal-secret")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nickname").value(nickname));
    }

    @Test
    @DisplayName("가입되지 않은 유저가 정보 조회를 요청하면 404로 응답한다.")
    void memberInfoNotFoundScenario() throws Exception {

        // given : 존재하지 않는 authId
        Long unknownAuthId = 999L;

        // when & then
        mockMvc.perform(get("/api/members/info")
                .header("X-User-Id", String.valueOf(unknownAuthId))
                .header("X-Internal-Secret", "test-internal-secret")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound());
    }
}
