package com.bds.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

/**
 * 게이트웨이가 심어주는 X-User-Id 등 신원 헤더는, 보안그룹으로 서비스 포트 직접 접근을
 * 막더라도 같은 네트워크 안에서는 누구나 흉내 낼 수 있다(1차 방어는 인프라 레벨 우회 가능).
 * 신원 헤더가 실려온 요청은 게이트웨이만 아는 내부 시크릿(X-Internal-Secret)이
 * 함께 있어야만 통과시켜, 위조된 신원 헤더를 애플리케이션 레벨에서 2차로 차단한다.
 * 서비스 간 내부 API(경로가 /internal/로 시작)는 신원 헤더를 실어 보내지 않으므로,
 * 신원 헤더 유무와 무관하게 항상 같은 시크릿을 요구해 보호한다.
 */
public class InternalGatewaySecretFilter extends OncePerRequestFilter {

    public static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private static final String[] IDENTITY_HEADERS = {"X-User-Id", "X-User-Email", "X-User-Roles"};
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    private final String expectedSecret;

    public InternalGatewaySecretFilter(String expectedSecret) {
        if (!StringUtils.hasText(expectedSecret)) {
            throw new IllegalStateException("internal.gateway-secret 값이 설정되지 않았습니다.");
        }
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        if (requiresSecret(request) && !isValidSecret(request.getHeader(INTERNAL_SECRET_HEADER))) {
            respondUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresSecret(HttpServletRequest request) {
        return claimsIdentity(request) || isInternalPath(request);
    }

    private boolean claimsIdentity(HttpServletRequest request) {
        for (String header : IDENTITY_HEADERS) {
            if (StringUtils.hasText(request.getHeader(header))) {
                return true;
            }
        }
        return false;
    }

    private boolean isInternalPath(HttpServletRequest request) {
        try {
            String decodedPath = UriUtils.decode(request.getRequestURI(), StandardCharsets.UTF_8);
            return decodedPath.startsWith(INTERNAL_PATH_PREFIX);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private boolean isValidSecret(String providedSecret) {
        if (!StringUtils.hasText(providedSecret)) {
            return false;
        }

        byte[] provided = providedSecret.getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedSecret.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(provided, expected);
    }

    private void respondUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
            "{\"code\":\"INVALID_INTERNAL_SECRET\",\"message\":\"게이트웨이를 통하지 않은 요청은 허용되지 않습니다.\"}"
        );
    }
}
