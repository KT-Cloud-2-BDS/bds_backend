package com.bds.auth.infrastructure.security;

import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Naver의 사용자 정보 응답은 실제 필드(id, email, nickname)가
 * {"resultcode": "00", "response": {...}} 형태로 "response" 안에 한 번 더 감싸져 있다.
 * Spring의 기본 OAuth2UserService는 top-level 속성만 attributes로 노출하므로,
 * "response"를 풀어서 평평한 attributes로 재구성해야 이후 코드에서 id/email 등을 바로 꺼낼 수 있다.
 * delegate를 상속이 아닌 위임으로 갖는 이유는, 실제 사용자 정보 조회(네트워크 호출)와
 * "response" 언래핑 로직을 분리해 언래핑 로직만 순수 단위테스트로 검증하기 위함이다.
 */
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    public CustomOAuth2UserService() {
        this(new DefaultOAuth2UserService());
    }

    CustomOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
        this.delegate = delegate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        Object responseObject = oAuth2User.getAttributes().get("response");
        if (!(responseObject instanceof Map)) {
            throw new OAuth2AuthenticationException("소셜 로그인 제공자로부터 사용자 정보를 받지 못했습니다.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> naverAttributes = (Map<String, Object>) responseObject;

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), naverAttributes, "id");
    }
}
