package com.bds.chat.infrastructure.session;

import com.bds.chat.application.chatRoom.ChatRoomAccessPolicy;
import com.bds.chat.infrastructure.security.JwtVerifier;
import com.bds.chat.infrastructure.security.TokenRenewalManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthChannelInterceptor 단위 테스트")
class AuthChannelInterceptorUnitTest {

    @Mock JwtVerifier jwtVerifier;
    @Mock SessionContextRegistry sessionContextRegistry;
    @Mock TokenRenewalManager renewalManager;
    @Mock ChatRoomAccessPolicy accessPolicy;
    @Mock MessageChannel channel;

    @InjectMocks AuthChannelInterceptor interceptor;

    private static final String SESSION_ID = "test-session";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(interceptor, "maxAnonymousSessions", 1000);
    }

    private Message<?> buildMessage(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(SESSION_ID);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildMessage(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(SESSION_ID);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildConnectWithAuth(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId(SESSION_ID);
        accessor.addNativeHeader("Authorization", authHeader);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Nested
    @DisplayName("CONNECT")
    class ConnectTest {

        @Test
        void 익명_CONNECT_슬롯_허용_시_메시지를_반환한다() {
            given(sessionContextRegistry.tryAcquireAnonymousSlot(SESSION_ID, 1000)).willReturn(true);
            Message<?> message = buildMessage(StompCommand.CONNECT);

            Message<?> result = interceptor.preSend(message, channel);

            assertThat(result).isNotNull();
        }

        @Test
        void 유효한_Bearer_토큰으로_인증_성공하면_메시지를_반환한다() {
            JwtVerifier.VerifiedToken token = new JwtVerifier.VerifiedToken(
                    "user-1", Set.of("ROLE_USER"), Instant.now().plusSeconds(3600));
            given(jwtVerifier.verify("valid-token")).willReturn(token);
            given(sessionContextRegistry.attachAuth(eq(SESSION_ID), eq("user-1"), any(), any())).willReturn(true);

            Message<?> result = interceptor.preSend(buildConnectWithAuth("Bearer valid-token"), channel);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("SUBSCRIBE")
    class SubscribeTest {

        @Test
        void 권한_있는_room_구독은_메시지를_반환한다() {
            given(sessionContextRegistry.authenticatedUserId(SESSION_ID)).willReturn(Optional.of("user-1"));
            given(accessPolicy.canSubscribe(eq(10L), any())).willReturn(true);

            Message<?> result = interceptor.preSend(
                    buildMessage(StompCommand.SUBSCRIBE, "/topic/chat.room.10"), channel);

            assertThat(result).isNotNull();
        }

        @Test
        void 인증된_세션의_user_queue_구독은_메시지를_반환한다() {
            given(sessionContextRegistry.authenticatedUserId(SESSION_ID)).willReturn(Optional.of("user-1"));

            Message<?> result = interceptor.preSend(
                    buildMessage(StompCommand.SUBSCRIBE, "/user/queue/notifications"), channel);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("SEND")
    class SendTest {

        @Test
        void refresh_destination_SEND는_인증_없이_통과한다() {
            Message<?> result = interceptor.preSend(
                    buildMessage(StompCommand.SEND, "/app/auth/refresh"), channel);

            assertThat(result).isNotNull();
        }

        @Test
        void 인증된_세션의_app_SEND는_메시지를_반환한다() {
            given(sessionContextRegistry.isAuthValid(SESSION_ID)).willReturn(true);

            Message<?> result = interceptor.preSend(
                    buildMessage(StompCommand.SEND, "/app/chat/send/1"), channel);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("기타 커맨드")
    class OtherCommandTest {

        @Test
        void DISCONNECT_커맨드는_그대로_통과한다() {
            Message<?> message = buildMessage(StompCommand.DISCONNECT);

            assertThatCode(() -> interceptor.preSend(message, channel))
                    .doesNotThrowAnyException();
        }
    }
}
