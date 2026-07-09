package com.bds.auth.integrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.bds.auth.infrastructure.persistence.adapter.RedisAdapter;

/**
 * [Auth 도메인 통합 테스트]
 * Member 서버로부터 내부 통신으로 넘어온 계정 생성 요청을 받아,
 * Redis 인증 티켓을 검증하고 최종적으로 Auth / AuthLocal DB에 적재하는 과정을 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("인증 서버 계정 생성 통합 테스트")
public class AuthCreateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private com.bds.auth.application.EmailService emailService;

    @MockitoBean
    private RedisAdapter redisAdapter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Member 서버로부터 계정 생성 요청이 오면, 이메일 인증을 확인하고 DB에 계정을 생성한다.")
    void authCreateLifecycleScenario() throws Exception {

        // given : 테스트 데이터 및 가짜 Redis 세팅
        String email = "yeojin@email.com";
        String password = "password123!";
        String redisKey = "verified:" + email;

        Mockito.when(redisAdapter.get(redisKey)).thenReturn("true");

        Map<String, String> authCreateRequest = new HashMap<>();
        authCreateRequest.put("email", email);
        authCreateRequest.put("password", password);

        String jsonContent = objectMapper.writeValueAsString(authCreateRequest);


        // when : 내부(Internal) API 컨트롤러 호출
        mockMvc.perform(post("/api/auths/account")
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
        org.junit.jupiter.api.Assertions.assertNotEquals(password, savedEncodedPassword, "비밀번호는 평문이 아니라 암호화 되어 저장되어야 합니다!");
    }
}