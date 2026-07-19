package com.bds.auth.infrastructure.security;

import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Naver의 사용자 정보 응답은 실제 필드(id, email, nickname)가
 * {"resultcode": "00", "response": {...}} 형태로 "response" 안에 한 번 더 감싸져 있다.
 * Spring의 기본 OAuth2UserService는 top-level 속성만 attributes로 노출하므로,
 * "response"를 풀어서 평평한 attributes로 재구성해야 이후 코드에서 id/email 등을 바로 꺼낼 수 있다.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Object responseObject = oAuth2User.getAttributes().get("response");
        if (!(responseObject instanceof Map)) {
            throw new OAuth2AuthenticationException("소셜 로그인 제공자로부터 사용자 정보를 받지 못했습니다.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> naverAttributes = (Map<String, Object>) responseObject;

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), naverAttributes, "id");
    }
}
