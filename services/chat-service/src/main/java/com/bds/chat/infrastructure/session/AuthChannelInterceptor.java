package com.bds.chat.infrastructure.session;

import com.bds.chat.application.chatRoom.ChatRoomAccessPolicy;
import com.bds.chat.infrastructure.config.StompPrincipal;
import com.bds.chat.infrastructure.security.JwtVerifier;
import com.bds.chat.infrastructure.security.TokenRenewalManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final String REFRESH_DESTINATION = "/app/auth/refresh";
    private static final String ROOM_TOPIC_PREFIX = "/topic/chat.room.";
    private static final String USER_QUEUE_PREFIX = "/user/queue/";

    private final JwtVerifier jwtVerifier;
    private final SessionContextRegistry sessionContextRegistry;
    private final TokenRenewalManager renewalManager;
    private final ChatRoomAccessPolicy accessPolicy;

    @Value("${app.auth.max-anonymous-sessions:1000}")
    private int maxAnonymousSessions;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel){
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if(accessor == null || accessor.getCommand() == null){
            return message;
        }

        switch (accessor.getCommand()){
            case CONNECT -> authenticateTokenPresent(accessor);
            case SUBSCRIBE -> authorizeSubscribe(accessor);
            case SEND -> guardSend(accessor);
            default -> {}
        }
        return message;
    }

    private void authenticateTokenPresent(StompHeaderAccessor accessor){
        String header = accessor.getFirstNativeHeader("Authorization");
        if(header == null){
            if(!sessionContextRegistry.tryAcquireAnonymousSlot(accessor.getSessionId(),maxAnonymousSessions)){
                log.warn("익명 CONNECT 거부 — 정원 초과 (max={})", maxAnonymousSessions);
                throw new MessagingException("익명 세션 정원 초과");
            }
            log.debug("익명 CONNECT 허용 sessionId={}", accessor.getSessionId());
            return;
        }
        if (!header.startsWith("Bearer ")) {
            throw new MessagingException("잘못된 Authorization 헤더 형식");
        }
        try {
            JwtVerifier.VerifiedToken token = jwtVerifier.verify(header.substring(7));
            String sessionId = accessor.getSessionId();
            if(!sessionContextRegistry.attachAuth(sessionId, token.userId(), token.roles(), token.expiresAt())){
                throw new MessagingException("세션이 이미 종료됨");
            }
            accessor.setUser(new StompPrincipal(token.userId(), token.roles()));
            renewalManager.schedule(sessionId,token.expiresAt());
            log.debug("STOMP 인증 성공 userId={} sessionId={} exp={}",token.userId(), sessionId, token.expiresAt());
        }catch (MessagingException e){
            throw e;
        }catch (Exception e){
            log.warn("STOMP 인증 실패: {}", e.getMessage());
            throw new MessagingException("JWT 검증 실패", e);
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor){
        String destination = accessor.getDestination();
        if(destination == null){
            throw new MessagingException("destination 누락");
        }
        Optional<String> userId = sessionContextRegistry.authenticatedUserId(accessor.getSessionId());
        if(destination.startsWith(ROOM_TOPIC_PREFIX)){
            Long roomId = parseRoomId(destination);
            if(!accessPolicy.canSubscribe(roomId,userId)){
                log.warn("구독 거부 destination={} userId={}", destination, userId.orElse("anonymous"));
                throw new MessagingException("구독 권한 없음 room=" + roomId);
            }
            return;
        }
        if(destination.startsWith(USER_QUEUE_PREFIX)){
            if(userId.isEmpty()){
                throw new MessagingException("익명 세션이 구독할 수 없는 destination: " + destination);
            }
            return;
        }
        log.warn("구독 거부 — 허용 목록 외 destination={} userId={}",
                destination, userId.orElse("anonymous"));
        throw new MessagingException("허용되지 않은 destination: " + destination);
    }

    private Long parseRoomId(String destination){
        String rest = destination.substring(ROOM_TOPIC_PREFIX.length());
        int index = rest.indexOf('.');
        String destinationRoomId = index == -1 ? rest : rest.substring(0,index);
        try {
            return Long.parseLong(destinationRoomId);
        }catch (NumberFormatException e){
            throw new MessagingException("잘못된 room destination: "+destination);
        }
    }

    private void guardSend(StompHeaderAccessor accessor){
        if(REFRESH_DESTINATION.equals(accessor.getDestination())){
            return;
        }
        if(!sessionContextRegistry.isAuthValid(accessor.getSessionId())){
            throw new MessagingException("인증되지 않았거나 만료된 세션의 message send");
        }
    }
}
