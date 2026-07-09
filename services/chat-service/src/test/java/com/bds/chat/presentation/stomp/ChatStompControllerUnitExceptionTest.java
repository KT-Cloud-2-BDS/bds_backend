package com.bds.chat.presentation.stomp;

import com.bds.chat.application.message.dto.MessageResponseDto;
import com.bds.chat.application.message.service.MessageService;
import com.bds.chat.common.DuplicateClientIdException;
import com.bds.chat.infrastructure.config.StompPrincipal;
import com.bds.chat.infrastructure.session.ReadReceiptBuffer;
import com.bds.chat.presentation.stomp.dto.ChatMessageDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatStompController 단위 예외 테스트")
class ChatStompControllerUnitExceptionTest {

    @Mock MessageService messageService;
    @Mock ReadReceiptBuffer readReceiptBuffer;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks ChatStompController controller;

    private static final StompPrincipal STOMP_PRINCIPAL = new StompPrincipal("5", Set.of("USER"));
    private static final Principal PLAIN_PRINCIPAL = () -> "5";

    @Nested
    @DisplayName("메시지 전송 예외")
    class SendExceptionTest {

        @Test
        @DisplayName("StompPrincipal이 아닌 principal이면 MessagingException을 던진다")
        void StompPrincipal이_아니면_MessagingException() {
            assertThatThrownBy(() -> controller.send(1L,
                    new ChatStompController.ChatSendRequest("cid", "msg"), PLAIN_PRINCIPAL))
                    .isInstanceOf(MessagingException.class);
        }

        @Test
        @DisplayName("메시지 저장 실패 시 예외가 전파되지 않고 오류 메시지를 /queue/error로 발송한다")
        void 메시지_저장_실패_시_오류_메시지_발송() {
            given(messageService.create(any(), anyLong()))
                    .willThrow(new RuntimeException("DB error"));

            assertThatCode(() -> controller.send(1L,
                    new ChatStompController.ChatSendRequest("cid", "msg"), STOMP_PRINCIPAL))
                    .doesNotThrowAnyException();

            verify(messagingTemplate).convertAndSendToUser(
                    eq("5"), eq("/queue/error"), any(ChatStompController.SendFailure.class));
        }

        @Test
        @DisplayName("DuplicateClientIdException 발생 시 기존 메시지를 브로드캐스트한다")
        void DuplicateClientIdException_발생_시_기존_메시지_브로드캐스트() {
            MessageResponseDto existing = new MessageResponseDto(
                    100L, 5L, "msg", false, LocalDateTime.of(2026, 1, 1, 0, 0), 1L);

            given(messageService.create(any(), anyLong()))
                    .willThrow(new DuplicateClientIdException("cid"));
            given(messageService.findByClientId("cid")).willReturn(existing);

            assertThatCode(() -> controller.send(1L,
                    new ChatStompController.ChatSendRequest("cid", "msg"), STOMP_PRINCIPAL))
                    .doesNotThrowAnyException();

            verify(messagingTemplate).convertAndSend(eq("/topic/chat.room.1"), any(ChatMessageDto.class));
        }
    }

    @Nested
    @DisplayName("읽음 처리 예외")
    class ReadExceptionTest {

        @Test
        @DisplayName("StompPrincipal이 아닌 principal이면 MessagingException을 던진다")
        void StompPrincipal이_아니면_MessagingException() {
            assertThatThrownBy(() -> controller.read(1L,
                    new ChatStompController.ReadRequest(10L), PLAIN_PRINCIPAL))
                    .isInstanceOf(MessagingException.class);
        }
    }
}
