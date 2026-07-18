package com.bds.auth.integrationTest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.repository.TokenCacheRepository;
import com.bds.auth.infrastructure.security.JwtTokenUtil;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Auth 도메인 통합 테스트 - 로그아웃]
 * 게이트웨이가 검증을 마친 요청(X-User-Id 헤더 + Authorization 헤더)을 받아,
 * refresh token을 삭제하고 access token을 블랙리스트에 등록하는 흐름을 검증합니다.
 */
@Transactional
@DisplayName("인증 서버 로그아웃 통합 테스트")
public class AuthLogoutIntegrationTest extends AbstractAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private com.bds.auth.application.EmailService emailService;

    @MockitoBean
    private TokenCacheRepository tokenCacheRepository;

    @Test
    @DisplayName("로그인된 유저가 로그아웃을 요청하면 refresh token을 삭제하고 access token을 블랙리스트에 등록한다.")
    void logoutSuccessScenario() throws Exception {

        // given : 로그인 상태를 흉내낸 access token 발급
        Long authId = 24L;
        String accessToken = jwtTokenUtil.createAccessToken(authId, "logout_test@email.com", Role.SUPPORTER);

        // when : 로그아웃 API 호출 (게이트웨이가 실어주는 헤더를 직접 흉내)
        mockMvc.perform(post("/api/auths/logout")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", String.valueOf(authId))
                .header("X-Internal-Secret", "test-internal-secret"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusMessage").value("로그아웃이 완료되었습니다."));

        // then
        verify(tokenCacheRepository, times(1)).delete("refresh:" + authId);
        verify(tokenCacheRepository, times(1))
            .save(eq("blacklist:" + accessToken), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS));
    }
}
