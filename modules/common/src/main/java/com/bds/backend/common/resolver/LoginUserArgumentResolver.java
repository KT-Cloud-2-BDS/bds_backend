package com.bds.backend.common.resolver;

import com.bds.backend.common.annotation.LoginUser;
import com.bds.backend.common.dto.CurrentUser;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
            && parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String userIdStr = webRequest.getHeader("X-User-Id");
        String email = webRequest.getHeader("X-User-Email");
        String role = webRequest.getHeader("X-User-Role");

        if (userIdStr == null) {
            throw new IllegalAccessException("인증된 유저 정보가 헤더에 존재하지 않습니다.");
        }

        Long userId = Long.valueOf(userIdStr);
        return new CurrentUser(userId, email, role);
    }

}
