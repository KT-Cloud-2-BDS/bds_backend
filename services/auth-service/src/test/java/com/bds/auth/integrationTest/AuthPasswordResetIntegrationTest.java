package com.bds.auth.integrationTest;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * [Auth 도메인 통합 테스트 - 비밀번호 재설정]
 * 이메일 인증으로 발급된 변경 권한 티켓이 유효할 때, DB의 비밀번호가
 * 새 값으로 암호화되어 반영되는지 검증합니다.
 */
@Transactional
@DisplayName("인증 서버 비밀번호 재설정 통합 테스트")
public class AuthPasswordResetIntegrationTest extends AbstractAuthIntegrationTest {

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
    @DisplayName("변경 권한 티켓이 유효하면 비밀번호가 새 값으로 암호화되어 반영된다.")
    void resetPasswordSuccessScenario() throws Exception {

        // given : 기존 회원 데이터 및 유효한 변경 권한 티켓 세팅
        String email = "reset_test@email.com";
        String oldEncodedPassword = "oldEncodedPassword!";
        String newPassword = "newPassword123!";

        em.createNativeQuery("INSERT INTO auth (email, status, role) VALUES (:email, 'ACTIVE', 'SUPPORTER')")
            .setParameter("email", email)
            .executeUpdate();

        Long authId = ((Number) em.createNativeQuery("SELECT id FROM auth WHERE email = :email")
            .setParameter("email", email)
            .getSingleResult()).longValue();

        em.createNativeQuery("INSERT INTO auth_local (auth_id, password) VALUES (:authId, :password)")
            .setParameter("authId", authId)
            .setParameter("password", oldEncodedPassword)
            .executeUpdate();

        em.flush();
        em.clear();

        Mockito.when(tokenCacheRepository.get("pw-reset-verified:" + email)).thenReturn("true");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);
        requestBody.put("newPassword", newPassword);
        String jsonContent = objectMapper.writeValueAsString(requestBody);

        // when : 비밀번호 재설정 API 호출
        mockMvc.perform(patch("/api/auths/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
            .andDo(print())
            .andExpect(status().isOk());

        // then : DB의 비밀번호가 평문 새 비밀번호와 다른(암호화된) 값으로 바뀌었는지 확인
        em.flush();
        em.clear();

        String savedPassword = (String) em.createNativeQuery(
                "SELECT al.password FROM auth_local al WHERE al.auth_id = :authId")
            .setParameter("authId", authId)
            .getSingleResult();

        assertNotNull(savedPassword);
        assertNotEquals(oldEncodedPassword, savedPassword, "비밀번호가 새 값으로 변경되어야 합니다.");
        assertNotEquals(newPassword, savedPassword, "비밀번호는 평문이 아니라 암호화되어 저장되어야 합니다.");
    }
}
