package com.bds.chat.infrastructure.persistence.message;

import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.MessageStatus;
import com.bds.chat.domain.message.MessageType;
import com.bds.chat.domain.shared.ChatMessageId;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageMapperUnitTest {

    private final ChatMessageMapper mapper = new ChatMessageMapper();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    private ChatRoomJpaEntity roomEntity() {
        return ChatRoomJpaEntity.builder()
                .id(10L).creatorId(2L).productId(1L)
                .status(ChatRoomStatus.ACTIVE).type(ChatRoomType.INQUIRY).createdAt(NOW)
                .build();
    }

    private ChatMessageJpaEntity messageEntity() {
        return ChatMessageJpaEntity.builder()
                .id(100L).room(roomEntity()).senderId(5L)
                .content("hello").type(MessageType.TEXT).status(MessageStatus.SENT)
                .createdAt(NOW).deletedAt(null).clientId("cm-1")
                .build();
    }

    @Nested
    @DisplayName("엔티티 → 도메인 변환")
    class ToDomainTest {

        @Test
        void 모든_필드를_올바르게_매핑한다() {
            ChatMessage result = mapper.toDomain(messageEntity());

            assertThat(result.getId().value()).isEqualTo(100L);
            assertThat(result.getRoomId().value()).isEqualTo(10L);
            assertThat(result.getSenderId().value()).isEqualTo(5L);
            assertThat(result.getContent()).isEqualTo("hello");
            assertThat(result.getType()).isEqualTo(MessageType.TEXT);
            assertThat(result.getClientId()).isEqualTo("cm-1");
            assertThat(result.getDeletedAt()).isNull();
        }

        @Test
        void senderId가_null이면_senderId가_null로_매핑된다() {
            ChatMessageJpaEntity entity = ChatMessageJpaEntity.builder()
                    .id(100L).room(roomEntity()).senderId(null)
                    .content("system").type(MessageType.SYSTEM).status(MessageStatus.SENT)
                    .createdAt(NOW).build();

            ChatMessage result = mapper.toDomain(entity);

            assertThat(result.getSenderId()).isNull();
        }

        @Test
        void 엔티티_ID가_0이면_INVALID_INPUT_예외() {
            ChatMessageJpaEntity entity = ChatMessageJpaEntity.builder()
                    .id(0L).room(roomEntity()).senderId(5L)
                    .content("hello").type(MessageType.TEXT).status(MessageStatus.SENT)
                    .createdAt(NOW).clientId("cm-1").build();

            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }
    }

    @Nested
    @DisplayName("도메인 → 엔티티 변환")
    class ToJpaEntityTest {

        @Test
        void 새_도메인의_모든_필드를_올바르게_매핑한다() {
            ChatMessage domain = ChatMessage.create(
                    ChatRoomId.of(10L), MemberId.of(5L), "hello", MessageType.TEXT, "cm-1", NOW);
            ChatRoomJpaEntity roomRef = roomEntity();

            ChatMessageJpaEntity result = mapper.toJpaEntity(domain, roomRef);

            assertThat(result.getId()).isNull();
            assertThat(result.getRoom().getId()).isEqualTo(10L);
            assertThat(result.getSenderId()).isEqualTo(5L);
            assertThat(result.getContent()).isEqualTo("hello");
            assertThat(result.getType()).isEqualTo(MessageType.TEXT);
            assertThat(result.getClientId()).isEqualTo("cm-1");
        }

        @Test
        void ID가_있는_도메인도_올바르게_매핑한다() {
            ChatMessage domain = ChatMessage.restore(
                    ChatMessageId.of(100L), ChatRoomId.of(10L), MemberId.of(5L),
                    "hello", MessageType.TEXT, "cm-1", MessageStatus.SENT, NOW, null);
            ChatRoomJpaEntity roomRef = roomEntity();

            ChatMessageJpaEntity result = mapper.toJpaEntity(domain, roomRef);

            assertThat(result.getId()).isEqualTo(100L);
        }
    }
}
