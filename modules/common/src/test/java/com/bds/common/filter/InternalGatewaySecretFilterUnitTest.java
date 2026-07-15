package com.bds.common.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("InternalGatewaySecretFilter 단위 테스트")
class InternalGatewaySecretFilterUnitTest {

    private final InternalGatewaySecretFilter filter = new InternalGatewaySecretFilter("correct-secret");

    @Test
    @DisplayName("신원 헤더가 없으면 내부 시크릿 없이도 통과시킨다")
    void 신원_헤더_없으면_통과() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/members/signup");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("신원 헤더와 올바른 내부 시크릿이 함께 오면 통과시킨다")
    void 올바른_시크릿이면_통과() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/members/info");
        request.addHeader("X-User-Id", "1");
        request.addHeader(InternalGatewaySecretFilter.INTERNAL_SECRET_HEADER, "correct-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("신원 헤더는 있는데 내부 시크릿이 없으면 401로 차단한다")
    void 시크릿_없으면_차단() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/members/info");
        request.addHeader("X-User-Id", "1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("신원 헤더는 있는데 내부 시크릿이 틀리면 401로 차단한다 (직접 포트 접근을 통한 위조 시나리오)")
    void 시크릿_틀리면_차단() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/members/info");
        request.addHeader("X-User-Id", "1");
        request.addHeader(InternalGatewaySecretFilter.INTERNAL_SECRET_HEADER, "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("내부 시크릿 값 없이는 필터를 생성할 수 없다")
    void 빈_시크릿_생성_실패() {
        assertThrows(IllegalStateException.class, () -> new InternalGatewaySecretFilter(" "));
    }
}
