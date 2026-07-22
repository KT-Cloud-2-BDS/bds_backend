package com.bds.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("OAuth2LoginFailureHandler 단위 테스트")
class OAuth2LoginFailureHandlerUnitTest {

    private final OAuth2LoginFailureHandler failureHandler = new OAuth2LoginFailureHandler();

    @Test
    @DisplayName("소셜 로그인 실패 시 프론트 redirect URI로 에러 표시와 함께 리다이렉트한다")
    public void 소셜로그인_실패_리다이렉트() throws Exception {
        // given
        ReflectionTestUtils.setField(failureHandler, "frontendRedirectUri", "http://localhost:3000/oauth/callback");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = new OAuth2AuthenticationException("provider error");

        // when
        failureHandler.onAuthenticationFailure(request, response, exception);

        // then
        ArgumentCaptor<String> redirectUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectUrlCaptor.capture());

        assertEquals("http://localhost:3000/oauth/callback#error=social_login_failed", redirectUrlCaptor.getValue());
    }
}
