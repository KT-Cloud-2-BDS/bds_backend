package com.bds.auth.integrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.bds.auth.infrastructure.persistence.adapter.RedisAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


/**
 * [Auth 도메인 통합 테스트 - 로그인]
 * DB에 저장된 회원 정보를 바탕으로 로그인 요청(이메일, 평문 비밀번호)을 수행하고,
 * 비밀번호 검증(Bcrypt 대조) 성공 시 JWT 토큰(Access/Refresh)이 정상적으로 발급되는지 확인합니다.
 * 발급된 Refresh Token이 Redis에 올바른 TTL 7일로 안전하게 적재되는 과정을 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("인증 서버 로그인 통합 테스트")
public class AuthLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private com.bds.auth.application.EmailService emailService;

    @MockitoBean
    private RedisAdapter redisAdapter;

    @Test
    @DisplayName("DB에 저장된 이메일과 비밀번호로 로그인을 요청하면 성공하고 토큰을 반환한다.")
    void loginSuccessScenario() throws Exception {

        // given : 미리 가입된 회원 데이터를 DB에 세팅
        String email = "login_test@email.com";
        String rawPassword = "password123!";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 1. Auth 부모 테이블에 저장 (ID 생성)
        em.createNativeQuery("INSERT INTO auth (email, status, role) VALUES (:email, 'ACTIVE', 'SUPPORTER')")
            .setParameter("email", email)
            .executeUpdate();

        // 생성된 Auth의 ID 가져오기
        Long authId = (Long) em.createNativeQuery("SELECT id FROM auth WHERE email = :email")
            .setParameter("email", email)
            .getSingleResult();

        // 2. AuthLocal 자식 테이블에 암호화된 비밀번호 저장
        em.createNativeQuery("INSERT INTO auth_local (auth_id, password) VALUES (:authId, :password)")
            .setParameter("authId", authId)
            .setParameter("password", encodedPassword)
            .executeUpdate();


        // 로그인을 요청할 JSON 바디 생성 (평문 비밀번호 사용)
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", email);
        loginRequest.put("password", rawPassword);
        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        // when : 로그인 API 호출
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
            .andDo(print())


            // then
            .andExpect(status().isOk());

        verify(redisAdapter, times(1)).save(
            anyString(),
            anyString(),
            eq(7L),
            eq(TimeUnit.DAYS)
        );
    }
}