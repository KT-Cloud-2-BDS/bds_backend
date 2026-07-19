package com.bds.auth.infrastructure.security;

import com.bds.auth.application.AuthService;
import com.bds.auth.presentation.dto.AuthLoginResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * OAuth2 로그인은 브라우저 리다이렉트로 끝나기 때문에 일반 REST 응답(JSON)을 내려줄 수 없다.
 * 발급한 우리 자체 JWT는 프론트 redirect URI로 다시 리다이렉트하면서 URL fragment(#)에 실어 보낸다.
 * fragment는 서버로 전송되지 않아 로그/Referer에 토큰이 남지 않는다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${oauth2.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        String provider = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();

        OAuth2User oAuth2User = oAuth2AuthenticationToken.getPrincipal();
        String providerId = oAuth2User.getName();
        String email = oAuth2User.getAttribute("email");

        AuthLoginResponseDto tokens = authService.processSocialLogin(provider, providerId, email);

        String redirectUrl = frontendRedirectUri + "#accessToken="
            + URLEncoder.encode(tokens.accessToken(), StandardCharsets.UTF_8)
            + "&refreshToken="
            + URLEncoder.encode(tokens.refreshToken(), StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
