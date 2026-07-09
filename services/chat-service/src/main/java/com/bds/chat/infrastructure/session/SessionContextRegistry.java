package com.bds.chat.infrastructure.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class SessionContextRegistry implements WebSocketHandlerDecoratorFactory {

    private final ConcurrentHashMap<String, SessionContext> contexts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> sessionIdsByUser = new ConcurrentHashMap<>();

    private final AtomicInteger anonymousCount = new AtomicInteger();


    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                contexts.put(session.getId(), new SessionContext(session));
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                SessionContext removed = contexts.remove(session.getId());
                if(removed!=null){
                    if(removed.anonymous){
                        anonymousCount.decrementAndGet();
                    }
                    if(removed.authState !=null){
                        Set<String> ids = sessionIdsByUser.get(removed.authState.userId());
                        if(ids!=null){
                            ids.remove(session.getId());
                        }
                    }
                }
                super.afterConnectionClosed(session, status);
            }
        };
    }

    public boolean attachAuth(String sessionId, String userId, Set<String> roles, Instant expiresAt) {
        SessionContext context = contexts.get(sessionId);
        if(context == null) {
            return false;
        }
        context.authState = new AuthState(userId, roles, expiresAt, true);
        sessionIdsByUser.computeIfAbsent(userId, k->ConcurrentHashMap.newKeySet()).add(sessionId);
        return true;
    }

    public void extendAuth(String sessionId, Instant newExpiresAt) {
        SessionContext context = contexts.get(sessionId);
        if(context != null && context.authState != null) {
            context.authState = new  AuthState(context.authState.userId(), context.authState.roles(), newExpiresAt, true);
        }
    }

    public void invalidateAuth(String sessionId) {
        SessionContext context = contexts.get(sessionId);
        if(context != null && context.authState != null) {
            context.authState = context.authState.invalidated();
        }
    }

    public boolean isAuthValid(String sessionId) {
        SessionContext context = contexts.get(sessionId);
        return context != null
                && context.authState != null
                && context.authState.valid()
                && context.authState.expiresAt().isAfter(Instant.now());
    }


    public Optional<String> authenticatedUserId(String sessionId){
        SessionContext context = contexts.get(sessionId);
        if(context==null || context.authState == null
                || !context.authState.valid()
                || !context.authState.expiresAt().isAfter(Instant.now())){
            return Optional.empty();
        }
        return Optional.of(context.authState.userId());
    }



    public Set<String> sessionsOfCp(String userId) {
        return Set.copyOf(sessionIdsByUser.getOrDefault(userId,Set.of()));
    }


    public void closeSession(String sessionId, CloseStatus status) {
        SessionContext context = contexts.get(sessionId);
        if(context == null) {
            return;
        }
        try {
            context.socket.close(status);
            log.info("세션 강제 종료 sessionId={} code={}", sessionId, status.getCode());
        }catch (Exception e){
            log.warn("세션 종료 실패 sessionId={}",sessionId,e);
        }
    }

    public boolean tryAcquireAnonymousSlot(String sessionId, int max) {
        SessionContext ctx = contexts.get(sessionId);
        if (ctx == null) {
            return false; // CONNECT 처리 중 소켓이 이미 닫힌 레이스
        }
        if (anonymousCount.incrementAndGet() > max) {
            anonymousCount.decrementAndGet();
            return false;
        }
        ctx.anonymous = true;
        return true;
    }

    public int anonymousSessionCount() {
        return anonymousCount.get();
    }

    // 가변 context
    private static final class SessionContext{
        private final WebSocketSession socket;
        private volatile AuthState authState;
        private volatile boolean anonymous;

        private SessionContext(WebSocketSession socket) {
            this.socket = socket;
        }
    }

    public record AuthState(String userId, Set<String> roles, Instant expiresAt, boolean valid) {
        AuthState invalidated() {
            return new AuthState(userId, roles, expiresAt, false);
        }
    }
}
