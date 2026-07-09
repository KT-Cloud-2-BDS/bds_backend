package com.bds.chat.infrastructure.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketSessionTerminator 단위 테스트")
class WebSocketSessionTerminatorUnitTest {

    @Mock SessionContextRegistry sessionContextRegistry;
    @Mock SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks WebSocketSessionTerminator terminator;

    @BeforeEach
    void injectTemplate() {
        // @Lazy @Autowired 필드는 @InjectMocks가 보장하지 않으므로 명시적으로 주입
        ReflectionTestUtils.setField(terminator, "simpMessagingTemplate", simpMessagingTemplate);
    }

    @Nested
    @DisplayName("terminateUser")
    class TerminateUserTest {

        @Test
        @DisplayName("활성 세션이 없으면 아무것도 하지 않는다")
        void 활성_세션이_없으면_아무것도_하지_않는다() {
            given(sessionContextRegistry.sessionsOfCp("user1")).willReturn(Set.of());

            terminator.terminateUser("user1", "TEST");

            verify(sessionContextRegistry, never()).invalidateAuth(any());
            verify(sessionContextRegistry, never()).closeSession(any(), any());
        }

        @Test
        @DisplayName("여러 세션이 있으면 각 세션을 모두 종료한다")
        void 여러_세션이_있으면_각_세션을_모두_종료한다() {
            given(sessionContextRegistry.sessionsOfCp("user1")).willReturn(Set.of("s1", "s2"));

            terminator.terminateUser("user1", "TEST");

            verify(sessionContextRegistry).invalidateAuth("s1");
            verify(sessionContextRegistry).invalidateAuth("s2");
            verify(sessionContextRegistry).closeSession("s1", CloseStatus.POLICY_VIOLATION);
            verify(sessionContextRegistry).closeSession("s2", CloseStatus.POLICY_VIOLATION);
        }
    }

    @Nested
    @DisplayName("terminateSession")
    class TerminateSessionTest {

        @Test
        @DisplayName("종료 통지 실패해도 예외가 전파되지 않고 세션 종료는 계속 진행된다")
        void 종료_통지_실패해도_세션_종료는_계속_진행된다() {
            doThrow(new RuntimeException("messaging failed"))
                    .when(simpMessagingTemplate)
                    .convertAndSendToUser(anyString(), anyString(), any(), anyMap());

            assertThatCode(() -> terminator.terminateSession("s1", "TEST"))
                    .doesNotThrowAnyException();

            verify(sessionContextRegistry).invalidateAuth("s1");
            verify(sessionContextRegistry).closeSession("s1", CloseStatus.POLICY_VIOLATION);
        }
    }
}
