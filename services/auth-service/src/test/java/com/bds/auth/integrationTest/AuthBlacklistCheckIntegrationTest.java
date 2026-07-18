package com.bds.auth.integrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.repository.TokenCacheRepository;
import com.bds.auth.infrastructure.security.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Auth 도메인 통합 테스트 - 블랙리스트 조회]
 * gateway-service가 로그인 요청마다 호출하는 내부 API로,
 * 로그아웃 처리된 access token인지 여부를 정확히 반환하는지 검증합니다.
 */
@Transactional
@DisplayName("인증 서버 블랙리스트 조회 통합 테스트")
public class AuthBlacklistCheckIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private com.bds.auth.application.EmailService emailService;

    @MockitoBean
    private TokenCacheRepository tokenCacheRepository;

    @Test
    @DisplayName("블랙리스트에 등록된 토큰으로 조회하면 true를 반환한다.")
    void blacklistedTokenReturnsTrue() throws Exception {
        String accessToken = jwtTokenUtil.createAccessToken(1L, "blacklist_test@email.com", Role.SUPPORTER);
        org.mockito.Mockito.when(tokenCacheRepository.get("blacklist:" + accessToken)).thenReturn("true");

        mockMvc.perform(get("/internal/auths/blacklist")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Internal-Secret", "test-internal-secret"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("블랙리스트에 없는 토큰으로 조회하면 false를 반환한다.")
    void notBlacklistedTokenReturnsFalse() throws Exception {
        String accessToken = jwtTokenUtil.createAccessToken(1L, "normal_test@email.com", Role.SUPPORTER);
        org.mockito.Mockito.when(tokenCacheRepository.get("blacklist:" + accessToken)).thenReturn(null);

        mockMvc.perform(get("/internal/auths/blacklist")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Internal-Secret", "test-internal-secret"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }
}
