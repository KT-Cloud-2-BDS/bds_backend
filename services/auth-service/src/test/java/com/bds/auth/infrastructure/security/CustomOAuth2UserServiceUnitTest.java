package com.bds.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@DisplayName("CustomOAuth2UserService 단위 테스트")
class CustomOAuth2UserServiceUnitTest {

    @SuppressWarnings("unchecked")
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = mock(OAuth2UserService.class);

    private final CustomOAuth2UserService customOAuth2UserService = new CustomOAuth2UserService(delegate);

    @Test
    @DisplayName("delegate가 Naver 응답을 반환하면 response 안의 필드를 풀어서 id/email을 바로 꺼낼 수 있는 OAuth2User를 반환한다")
    public void 응답언래핑_성공() {
        // given
        Map<String, Object> naverResponse = Map.of("id", "naver-12345", "email", "social@email.com");
        Map<String, Object> rawAttributes = Map.of(
            "resultcode", "00",
            "message", "success",
            "response", naverResponse
        );
        OAuth2User rawOAuth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            rawAttributes,
            "resultcode"
        );
        given(delegate.loadUser(null)).willReturn(rawOAuth2User);

        // when
        OAuth2User result = customOAuth2UserService.loadUser(null);

        // then
        assertEquals("naver-12345", result.getName());
        assertEquals("naver-12345", result.getAttribute("id"));
        assertEquals("social@email.com", result.getAttribute("email"));
    }

    @Test
    @DisplayName("delegate 응답에 response 필드가 없으면 OAuth2AuthenticationException이 터진다")
    public void 응답에_response없음_예외() {
        // given
        Map<String, Object> rawAttributes = Map.of("resultcode", "99", "message", "fail");
        OAuth2User rawOAuth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            rawAttributes,
            "resultcode"
        );
        given(delegate.loadUser(null)).willReturn(rawOAuth2User);

        // when & then
        assertThrows(OAuth2AuthenticationException.class, () -> customOAuth2UserService.loadUser(null));
    }
}
