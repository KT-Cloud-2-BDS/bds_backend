package com.bds.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bds.auth.application.AuthService;
import com.bds.auth.presentation.dto.AuthLoginResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2LoginSuccessHandler 단위 테스트")
class OAuth2LoginSuccessHandlerUnitTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private OAuth2LoginSuccessHandler successHandler;

    @Test
    @DisplayName("소셜 로그인 성공 시 발급된 토큰을 fragment에 담아 프론트로 리다이렉트한다")
    public void 소셜로그인_성공_리다이렉트() throws Exception {
        // given
        ReflectionTestUtils.setField(successHandler, "frontendRedirectUri", "http://localhost:3000/oauth/callback");

        OAuth2User oAuth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of("id", "naver-12345", "email", "social@email.com"),
            "id"
        );
        OAuth2AuthenticationToken authentication =
            new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(), "naver");

        given(authService.processSocialLogin("naver", "naver-12345", "social@email.com"))
            .willReturn(new AuthLoginResponseDto("accessToken", "refreshToken"));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // when
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<String> redirectUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectUrlCaptor.capture());

        String redirectUrl = redirectUrlCaptor.getValue();
        assertTrue(redirectUrl.startsWith("http://localhost:3000/oauth/callback#accessToken=accessToken"));
        assertTrue(redirectUrl.contains("refreshToken=refreshToken"));
    }
}
