package com.bds.chat.domain.message;

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

@ExtendWith(MockitoExtension.class)
class ChatMessageUnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final ChatRoomId ROOM_ID = ChatRoomId.of(1L);
    private static final MemberId SENDER_ID = MemberId.of(2L);

    @Nested
    @DisplayName("메시지 생성")
    class CreateTest {

        @Test
        void 메시지_생성시_SENT_상태로_생성된다() {
            ChatMessage message = ChatMessage.create(ROOM_ID, SENDER_ID, "hello", MessageType.TEXT, "cm-1", NOW);
            assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
        }

        @Test
        void 메시지_생성시_id가_null이다() {
            ChatMessage message = ChatMessage.create(ROOM_ID, SENDER_ID, "hello", MessageType.TEXT, "cm-1", NOW);
            assertThat(message.getId()).isNull();
        }

        @Test
        void 메시지_생성시_모든_필드가_올바르게_설정된다() {
            ChatMessage message = ChatMessage.create(ROOM_ID, SENDER_ID, "hello", MessageType.TEXT, "cm-1", NOW);
            assertThat(message.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(message.getSenderId()).isEqualTo(SENDER_ID);
            assertThat(message.getContent()).isEqualTo("hello");
            assertThat(message.getType()).isEqualTo(MessageType.TEXT);
            assertThat(message.getClientId()).isEqualTo("cm-1");
            assertThat(message.getCreatedAt()).isEqualTo(NOW);
            assertThat(message.getDeletedAt()).isNull();
        }

        @Test
        void 시스템_메시지_생성시_senderId가_null이다() {
            ChatMessage message = ChatMessage.createSystem(ROOM_ID, "system message", NOW);
            assertThat(message.getSenderId()).isNull();
            assertThat(message.getType()).isEqualTo(MessageType.SYSTEM);
            assertThat(message.getClientId()).isNull();
        }
    }

    @Nested
    @DisplayName("메시지 복원")
    class RestoreTest {

        @Test
        void restore로_모든_필드가_올바르게_복원된다() {
            ChatMessageId id = ChatMessageId.of(10L);
            ChatMessage message = ChatMessage.restore(id, ROOM_ID, SENDER_ID, "hello",
                    MessageType.TEXT, "cm-1", MessageStatus.SENT, NOW, null);
            assertThat(message.getId()).isEqualTo(id);
            assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
        }
    }

    @Nested
    @DisplayName("ID 할당")
    class AssignIdTest {

        @Test
        void id_할당이_성공한다() {
            ChatMessage message = ChatMessage.create(ROOM_ID, SENDER_ID, "hello", MessageType.TEXT, "cm-1", NOW);
            ChatMessageId id = ChatMessageId.of(99L);
            message.assignId(id);
            assertThat(message.getId()).isEqualTo(id);
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteTest {

        @Test
        void SENT_메시지_삭제시_DELETED_상태가_된다() {
            ChatMessage message = ChatMessage.create(ROOM_ID, SENDER_ID, "hello", MessageType.TEXT, "cm-1", NOW);
            message.delete(NOW);
            assertThat(message.getStatus()).isEqualTo(MessageStatus.DELETED);
        }

        @Test
        void SENT_메시지_삭제시_deletedAt이_설정된다() {
            ChatMessage message = ChatMessage.create(ROOM_ID, SENDER_ID, "hello", MessageType.TEXT, "cm-1", NOW);
            message.delete(NOW);
            assertThat(message.getDeletedAt()).isEqualTo(NOW);
        }

        @Test
        void 이미_삭제된_메시지는_재삭제해도_예외없이_무시된다() {
            ChatMessage message = ChatMessage.restore(ChatMessageId.of(1L), ROOM_ID, SENDER_ID, "hello",
                    MessageType.TEXT, "cm-1", MessageStatus.DELETED, NOW, NOW);
            message.delete(NOW.plusMinutes(1));
            assertThat(message.getDeletedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("삭제 여부 확인")
    class IsDeletedTest {

        @Test
        void SENT_상태이면_isDeleted가_false이다() {
            ChatMessage message = ChatMessage.create(ROOM_ID, SENDER_ID, "hello", MessageType.TEXT, "cm-1", NOW);
            assertThat(message.isDeleted()).isFalse();
        }

        @Test
        void DELETED_상태이면_isDeleted가_true이다() {
            ChatMessage message = ChatMessage.restore(ChatMessageId.of(1L), ROOM_ID, SENDER_ID, "hello",
                    MessageType.TEXT, "cm-1", MessageStatus.DELETED, NOW, NOW);
            assertThat(message.isDeleted()).isTrue();
        }
    }
}
