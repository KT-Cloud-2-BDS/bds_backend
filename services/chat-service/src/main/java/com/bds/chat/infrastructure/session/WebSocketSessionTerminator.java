package com.bds.chat.infrastructure.session;

import com.bds.chat.application.session.service.SessionTerminationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionTerminator implements SessionTerminationService {
    private final SessionContextRegistry sessionContextRegistry;

    @Lazy
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void terminateUser(String userId, String reason) {
        Set<String> sessionIds = sessionContextRegistry.sessionsOfCp(userId);
        if(sessionIds.isEmpty()){
            return;
        }
        log.warn("유저 강제 종료 userId={} reason={} sessions={}", userId, reason, sessionIds.size());
        sessionIds.forEach(sessionId -> terminateSession(sessionId, reason));
    }

    @Override
    public void terminateSession(String sessionId, String reason) {
        sessionContextRegistry.invalidateAuth(sessionId);
        notifySession(sessionId, reason);
        sessionContextRegistry.closeSession(sessionId, CloseStatus.POLICY_VIOLATION);
    }

    private void notifySession(String sessionId, String reason) {
        try {
            SimpMessageHeaderAccessor header = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            header.setSessionId(sessionId);
            header.setLeaveMutable(true);
            simpMessagingTemplate.convertAndSendToUser(sessionId, "/queue/auth",
                    Map.of("type", "SESSION_TERMINATED", "reason", reason),
                    header.getMessageHeaders()
            );
        }catch (Exception e){
            log.debug("종료 통지 실패(무시) sessionId={}", sessionId);
        }
    }
}
