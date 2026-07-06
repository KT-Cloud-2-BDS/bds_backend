package com.bds.order.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // TODO: 추후 설정될 실제 Gateway에 맞게 header 이름 변경
        String memberIdHeader = request.getHeader("X-Member-Id");

        if (memberIdHeader == null || memberIdHeader.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            Long memberId = Long.parseLong(memberIdHeader);
            var auth = new UsernamePasswordAuthenticationToken(memberId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}