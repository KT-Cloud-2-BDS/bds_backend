package com.bds.chat.domain.message;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.shared.ChatMessageId;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ChatMessageUnitExceptionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final ChatRoomId ROOM_ID = ChatRoomId.of(1L);
    private static final MemberId SENDER_ID = MemberId.of(2L);

    @Nested
    @DisplayName("메시지 생성 예외")
    class CreateExceptionTest {

        @Test
        void roomId가_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> ChatMessage.create(null, SENDER_ID, "hello", MessageType.TEXT, "cm-1", NOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void content가_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> ChatMessage.create(ROOM_ID, SENDER_ID, null, MessageType.TEXT, "cm-1", NOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void type이_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> ChatMessage.create(ROOM_ID, SENDER_ID, "hello", null, "cm-1", NOW))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("ID 할당 예외")
    class AssignIdExceptionTest {

        @Test
        void 이미_id가_있으면_CONFLICT_BusinessException이_발생한다() {
            ChatMessage message = ChatMessage.restore(ChatMessageId.of(1L), ROOM_ID, SENDER_ID, "hello",
                    MessageType.TEXT, "cm-1", MessageStatus.SENT, NOW, null);
            assertThatThrownBy(() -> message.assignId(ChatMessageId.of(2L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }

        @Test
        void assignId에_null을_전달하면_NullPointerException이_발생한다() {
            ChatMessage message = ChatMessage.create(ROOM_ID, SENDER_ID, "hello", MessageType.TEXT, "cm-1", NOW);
            assertThatThrownBy(() -> message.assignId(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("메시지 복원 예외")
    class RestoreExceptionTest {

        @Test
        void id가_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> ChatMessage.restore(null, ROOM_ID, SENDER_ID, "hello",
                    MessageType.TEXT, "cm-1", MessageStatus.SENT, NOW, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
