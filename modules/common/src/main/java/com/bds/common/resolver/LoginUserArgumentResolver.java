package com.bds.common.resolver;

import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

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
        String rolesHeader = webRequest.getHeader("X-User-Roles");

        if (userIdStr == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 유저 정보가 헤더에 존재하지 않습니다.");
        }

        Long userId;

        try {
            userId = Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 사용자 ID 형식입니다.");
        }

        List<String> roles = StringUtils.hasText(rolesHeader)
            ? Arrays.asList(rolesHeader.split(","))
            : List.of();

        return new CurrentUser(userId, email, roles);
    }

}
