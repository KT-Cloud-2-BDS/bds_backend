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
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthChannelInterceptor 예외 단위 테스트")
class AuthChannelInterceptorUnitExceptionTest {

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
    @DisplayName("CONNECT 예외")
    class ConnectExceptionTest {

        @Test
        void 익명_세션_정원_초과_시_MessagingException을_던진다() {
            given(sessionContextRegistry.tryAcquireAnonymousSlot(SESSION_ID, 1000)).willReturn(false);

            assertThatThrownBy(() -> interceptor.preSend(buildMessage(StompCommand.CONNECT), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("익명 세션 정원 초과");
        }

        @Test
        void Bearer_없는_Authorization_헤더는_MessagingException을_던진다() {
            assertThatThrownBy(() -> interceptor.preSend(buildConnectWithAuth("Basic abc123"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("잘못된 Authorization 헤더 형식");
        }

        @Test
        void JWT_검증_중_일반_예외_발생_시_JWT_검증_실패_MessagingException을_던진다() {
            given(jwtVerifier.verify(any())).willThrow(new RuntimeException("파싱 오류"));

            assertThatThrownBy(() -> interceptor.preSend(buildConnectWithAuth("Bearer bad-token"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("JWT 검증 실패");
        }

        @Test
        void JWT_검증_중_MessagingException은_그대로_재전파된다() {
            given(jwtVerifier.verify(any())).willThrow(new MessagingException("서명 불일치"));

            assertThatThrownBy(() -> interceptor.preSend(buildConnectWithAuth("Bearer bad-token"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("서명 불일치");
        }

        @Test
        void attachAuth_실패_시_세션_종료_MessagingException을_던진다() {
            JwtVerifier.VerifiedToken token = new JwtVerifier.VerifiedToken(
                    "user-1", Set.of(), Instant.now().plusSeconds(3600));
            given(jwtVerifier.verify(any())).willReturn(token);
            given(sessionContextRegistry.attachAuth(eq(SESSION_ID), eq("user-1"), any(), any())).willReturn(false);

            assertThatThrownBy(() -> interceptor.preSend(buildConnectWithAuth("Bearer valid-token"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("세션이 이미 종료됨");
        }
    }

    @Nested
    @DisplayName("SUBSCRIBE 예외")
    class SubscribeExceptionTest {

        @Test
        void destination_누락_시_MessagingException을_던진다() {
            assertThatThrownBy(() -> interceptor.preSend(buildMessage(StompCommand.SUBSCRIBE), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("destination 누락");
        }

        @Test
        void room_구독_권한_없을_때_MessagingException을_던진다() {
            given(sessionContextRegistry.authenticatedUserId(SESSION_ID)).willReturn(Optional.empty());
            given(accessPolicy.canSubscribe(eq(10L), any())).willReturn(false);

            assertThatThrownBy(() -> interceptor.preSend(
                    buildMessage(StompCommand.SUBSCRIBE, "/topic/chat.room.10"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("구독 권한 없음");
        }

        @Test
        void roomId가_숫자가_아니면_MessagingException을_던진다() {
            given(sessionContextRegistry.authenticatedUserId(SESSION_ID)).willReturn(Optional.of("user-1"));

            assertThatThrownBy(() -> interceptor.preSend(
                    buildMessage(StompCommand.SUBSCRIBE, "/topic/chat.room.abc"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("잘못된 room destination");
        }

        @Test
        void 익명_세션이_user_queue_구독_시_MessagingException을_던진다() {
            given(sessionContextRegistry.authenticatedUserId(SESSION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> interceptor.preSend(
                    buildMessage(StompCommand.SUBSCRIBE, "/user/queue/notifications"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("익명 세션이 구독할 수 없는 destination");
        }

        @Test
        void 허용_목록_외_destination_구독_시_MessagingException을_던진다() {
            given(sessionContextRegistry.authenticatedUserId(SESSION_ID)).willReturn(Optional.of("user-1"));

            assertThatThrownBy(() -> interceptor.preSend(
                    buildMessage(StompCommand.SUBSCRIBE, "/unknown/path"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("허용되지 않은 destination");
        }
    }

    @Nested
    @DisplayName("SEND 예외")
    class SendExceptionTest {

        @Test
        void destination_누락_시_MessagingException을_던진다() {
            assertThatThrownBy(() -> interceptor.preSend(buildMessage(StompCommand.SEND), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("허용되지 않은 SEND destination");
        }

        @Test
        void app_외_destination_SEND_시_MessagingException을_던진다() {
            assertThatThrownBy(() -> interceptor.preSend(
                    buildMessage(StompCommand.SEND, "/topic/chat.room.1"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("허용되지 않은 SEND destination");
        }

        @Test
        void 인증_만료_세션의_SEND_시_MessagingException을_던진다() {
            given(sessionContextRegistry.isAuthValid(SESSION_ID)).willReturn(false);

            assertThatThrownBy(() -> interceptor.preSend(
                    buildMessage(StompCommand.SEND, "/app/chat/send/1"), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("인증되지 않았거나 만료된 세션");
        }
    }
}
