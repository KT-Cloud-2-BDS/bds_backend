package com.bds.chat.infrastructure.security;

import com.bds.chat.infrastructure.session.SessionContextRegistry;
import com.bds.chat.application.session.service.SessionTerminationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenRenewalManager {

    private final TaskScheduler authTaskScheduler;
    private final SessionContextRegistry sessionContextRegistry;
    private final SessionTerminationService sessionTermination;
    private final Map<String, ScheduledFuture<?>> renewalTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> killTasks = new ConcurrentHashMap<>();

    @Lazy
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Value("${app.auth.renew-before:PT2M}")
    private Duration renewBefore;
    @Value("${app.auth.renew-grace:PT1M}")
    private Duration grace;

    public void schedule(String sessionId, Instant expiresAt){
        cancel(sessionId);
        Instant requestAt = expiresAt.minus(renewBefore);
        if(!requestAt.isAfter(Instant.now())){
            requestRenewal(sessionId);
        }else {
            renewalTasks.put(sessionId, authTaskScheduler.schedule(()->
                    requestRenewal(sessionId),requestAt)
            );
        }
    }

    public void onRefreshed(String sessionId, Instant newExpiresAt){
        cancel(sessionId);
        sessionContextRegistry.extendAuth(sessionId, newExpiresAt);
        sendToSession(sessionId,Map.of("type","TOKEN_REFRESHED"));
        schedule(sessionId, newExpiresAt);
        log.debug("토큰 갱신 완료 sessionId={} newExp={}", sessionId, newExpiresAt);
    }


    @EventListener
    public void onDisconnect(SessionDisconnectEvent event){
        cancel(event.getSessionId());
    }

    private void requestRenewal(String sessionId){
        if(!sessionContextRegistry.isAuthValid(sessionId)){
            return;
        }
        sendToSession(sessionId,
                Map.of(
                        "type", "TOKEN_RENEWAL_REQUIRED",
                        "deadlineSeconds", grace.toSeconds()
                ));
        killTasks.put(sessionId,authTaskScheduler.schedule(()->
            expire(sessionId), Instant.now().plus(grace)
        ));
        log.debug("토큰 갱신 요청 발송 sessionId={} grace={}s", sessionId, grace.toSeconds());
    }

    private void expire(String sessionId){
        log.info("갱신 시한 초과 - 세션 종료 sessionid={}",sessionId);
        sessionTermination.terminateSession(sessionId, "TOKEN_EXPIRED");
    }

    private void sendToSession(String sessionId, Map<String, Object> payload){
        SimpMessageHeaderAccessor header = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        header.setSessionId(sessionId);
        header.setLeaveMutable(true);
        simpMessagingTemplate.convertAndSendToUser(sessionId,"/queue/auth",
                payload, header.getMessageHeaders());
    }

    private void cancel(String sessionId) {
        cancelTask(renewalTasks, sessionId);
        cancelTask(killTasks, sessionId);
    }

    private void cancelTask(Map<String, ScheduledFuture<?>> tasks, String sessionId) {
        ScheduledFuture<?> removed = tasks.remove(sessionId);
        if (removed != null) {
            removed.cancel(false);
        }
    }
}
