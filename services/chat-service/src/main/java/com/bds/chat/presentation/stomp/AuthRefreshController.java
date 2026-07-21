package com.bds.chat.presentation.stomp;

import com.bds.chat.infrastructure.security.JwtVerifier;
import com.bds.chat.infrastructure.security.TokenRenewalManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthRefreshController {
    private final JwtVerifier jwtVerifier;
    private final TokenRenewalManager tokenRenewalManager;

    @MessageMapping("/auth/refresh")
    public void refresh(@Payload RefreshRequest request,
                        SimpMessageHeaderAccessor accessor,
                        Principal principal) {
        String sessionId = accessor.getSessionId();
        try {
            JwtVerifier.VerifiedToken token = jwtVerifier.verify(request.token());

            // 세션 주인과 토큰 주인이 같아야 함 — 남의 토큰으로 세션 연장 방지
            if (!token.userId().equals(principal.getName())) {
                log.warn("갱신 토큰 subject 불일치 session={} principal={} token={}",
                        sessionId, principal.getName(), token.userId());
                return;
            }
            tokenRenewalManager.onRefreshed(sessionId, token.expiresAt());
        } catch (Exception e) {
            log.warn("토큰 갱신 검증 실패 sessionId={}: {}", sessionId, e.getMessage());
            // kill 타이머 유지 → grace 초과 시 disconnect
        }
    }

    public record RefreshRequest(String token) {}
}
