package com.bds.auth.integrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bds.auth.domain.repository.TokenCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Auth 도메인 통합 테스트]
 * Member 서버로부터 내부 통신으로 넘어온 계정 생성 요청을 받아,
 * Redis 인증 티켓을 검증하고 최종적으로 Auth / AuthLocal DB에 적재하는 과정을 검증합니다.
 */
@Transactional
@DisplayName("인증 서버 계정 생성 통합 테스트")
public class AuthCreateIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private com.bds.auth.application.EmailService emailService;

    @MockitoBean
    private TokenCacheRepository tokenCacheRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("신규 유저 가입: Redis 인증이 확인되면 DB에 완전히 새로운 계정을 생성한다.")
    void authCreateNewAccountScenario() throws Exception {

        // given : 테스트 데이터 및 가짜 Redis 세팅
        String email = "yeojin@email.com";
        String password = "password123!";
        String redisKey = "verified:" + email;

        Mockito.when(tokenCacheRepository.get(redisKey)).thenReturn("true");

        Map<String, String> authCreateRequest = new HashMap<>();
        authCreateRequest.put("email", email);
        authCreateRequest.put("password", password);

        String jsonContent = objectMapper.writeValueAsString(authCreateRequest);


        // when : 내부 API 컨트롤러 호출 (신규 가입)
        mockMvc.perform(post("/internal/auths/account")
                .header("X-Internal-Secret", "test-internal-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
            .andDo(print())
            .andExpect(status().isOk());


        // then : DB 정합성 및 암호화 검증
        // 1. Auth 테이블에 이메일이 잘 들어갔는지 Native Query로 확인
        String savedEmail = (String) em.createNativeQuery("SELECT email FROM auth WHERE email = :email")
            .setParameter("email", email)
            .getSingleResult();

        org.junit.jupiter.api.Assertions.assertEquals(email, savedEmail, "Auth 테이블에 이메일이 저장되어야 합니다.");

        // 2. AuthLocal 테이블에 비밀번호가 암호화되어 들어갔는지 확인
        String savedEncodedPassword = (String) em.createNativeQuery(
                "SELECT al.password FROM auth_local al JOIN auth a ON al.auth_id = a.id WHERE a.email = :email"
            )
            .setParameter("email", email)
            .getSingleResult();

        org.junit.jupiter.api.Assertions.assertNotNull(savedEncodedPassword);
        org.junit.jupiter.api.Assertions.assertNotEquals(password, savedEncodedPassword, "비밀번호는 평문이 아니라 암호화 되어 저장되어야 합니다.");
    }

    @Test
    @DisplayName("기존 탈퇴 유저 가입: 이미 DELETED 상태로 존재하는 계정이 있다면, 복구(ACTIVE)하여 처리한다.")
    void authRestoreAccountScenario() throws Exception {

        // given : 1. DB에 기존 탈퇴 회원 데이터 적재
        String email = "jinjinjala312@naver.com";
        String password = "password123!";

        em.createNativeQuery("INSERT INTO auth (email, role, status) VALUES (:email, 'SUPPORTER', 'DELETED')")
            .setParameter("email", email)
            .executeUpdate();

        em.flush();
        em.clear();

        // 2. 가짜 Redis 및 요청 바디 세팅
        String redisKey = "verified:" + email;
        Mockito.when(tokenCacheRepository.get(redisKey)).thenReturn("true");

        Map<String, String> authCreateRequest = new HashMap<>();
        authCreateRequest.put("email", email);
        authCreateRequest.put("password", password);

        String jsonContent = objectMapper.writeValueAsString(authCreateRequest);


        // when : 내부 API 호출 (탈퇴 유저가 동일 이메일로 재가입 시도)
        mockMvc.perform(post("/internal/auths/account")
                .header("X-Internal-Secret", "test-internal-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
            .andDo(print())
            .andExpect(status().isOk());


        // then : 데이터가 새로 insert된 게 아니라 기존 데이터의 status가 ACTIVE로 '복구'되었는지 검증
        String currentStatus = (String) em.createNativeQuery("SELECT status FROM auth WHERE email = :email")
            .setParameter("email", email)
            .getSingleResult();

        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", currentStatus, "기존 DELETED 상태였던 계정이 ACTIVE 상태로 복구되어야 합니다.");

        // 비밀번호 매칭 검증
        String savedEncodedPassword = (String) em.createNativeQuery(
                "SELECT al.password FROM auth_local al JOIN auth a ON al.auth_id = a.id WHERE a.email = :email"
            )
            .setParameter("email", email)
            .getSingleResult();

        org.junit.jupiter.api.Assertions.assertNotNull(savedEncodedPassword);
        org.junit.jupiter.api.Assertions.assertNotEquals(password, savedEncodedPassword, "복구 시에도 비밀번호는 암호화 되어 저장되어야 합니다.");
    }
}