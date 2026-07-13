package com.bds.auth.integrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bds.auth.domain.repository.TokenCacheRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Auth 도메인 통합 테스트 - 탈퇴]
 * Member 서버에서 내부 통신으로 전달된 계정 탈퇴 요청을 받아,
 * Auth 및 AuthLocal DB에서 해당 회원의 인증 정보가 정상적으로 삭제되는지 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("인증 서버 탈퇴 통합 테스트")
public class AuthDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private com.bds.auth.application.EmailService emailService;

    @MockitoBean
    private TokenCacheRepository tokenCacheRepository;

    @Test
    @DisplayName("내부 통신으로 계정 삭제 요청이 오면, DB에서 해당 인증 정보가 성공적으로 삭제된다.")
    void deleteAuthSuccessScenario() throws Exception {

        // given : 삭제할 타겟 회원 데이터 세팅
        String email = "bye_test@email.com";

        // 1. Auth 부모 테이블에 임의의 회원 저장
        em.createNativeQuery("INSERT INTO auth (email, status, role) VALUES (:email, 'ACTIVE', 'SUPPORTER')")
            .setParameter("email", email)
            .executeUpdate();

        // 방금 만든 회원의 ID(authId) 가져오기
        Long authId = ((Number) em.createNativeQuery("SELECT id FROM auth WHERE email = :email")
            .setParameter("email", email)
            .getSingleResult()).longValue();

        // 2. AuthLocal 자식 테이블에도 데이터 저장
        em.createNativeQuery("INSERT INTO auth_local (auth_id, password) VALUES (:authId, 'dummyPassword!')")
            .setParameter("authId", authId)
            .executeUpdate();


        // when : 탈퇴 API 호출
        mockMvc.perform(delete("/api/auths/" + authId))
            .andDo(print())

            // then : 검증 (HTTP 상태 200 & DB 확인)
            .andExpect(status().isOk());

        em.flush();
        em.clear();

        // [검증 1] Auth 테이블: soft delete
        String status = (String) em.createNativeQuery("SELECT status FROM auth WHERE id = :authId")
            .setParameter("authId", authId)
            .getSingleResult();
        assertEquals("DELETED", status, "Auth 테이블의 상태가 DELETED로 변경되어야 합니다.");

        // [검증 2] AuthLocal 테이블: 물리적 삭제
        Number localCount = (Number) em.createNativeQuery("SELECT COUNT(*) FROM auth_local WHERE auth_id = :authId")
            .setParameter("authId", authId)
            .getSingleResult();
        assertEquals(0, localCount.intValue(), "AuthLocal 테이블에서 비밀번호 등은 완전히 삭제되어야 합니다.");
    }
}