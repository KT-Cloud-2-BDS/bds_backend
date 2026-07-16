package com.bds.chat.infrastructure.persistence.message;

import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.MessageStatus;
import com.bds.chat.domain.message.MessageType;
import com.bds.chat.domain.shared.ChatMessageId;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatMessagePersistenceAdapterUnitTest {

    @Mock ChatMessageJpaRepository jpaRepository;
    @Mock ChatMessageMapper mapper;
    @Mock EntityManager entityManager;

    @InjectMocks ChatMessagePersistenceAdapter adapter;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
    }

    private ChatRoomJpaEntity roomEntity() {
        return ChatRoomJpaEntity.builder()
                .id(10L).creatorId(2L).productId(1L)
                .status(ChatRoomStatus.ACTIVE).type(ChatRoomType.INQUIRY).createdAt(NOW)
                .build();
    }

    private ChatMessageJpaEntity messageEntity(Long id) {
        return ChatMessageJpaEntity.builder()
                .id(id).room(roomEntity()).senderId(5L)
                .content("hello").type(MessageType.TEXT).status(MessageStatus.SENT)
                .createdAt(NOW).deletedAt(null).clientId("cm-1")
                .build();
    }

    private ChatMessage messageDomain(Long id) {
        if (id == null) return ChatMessage.create(ChatRoomId.of(10L), MemberId.of(5L), "hello", MessageType.TEXT, "cm-1", NOW);
        return ChatMessage.restore(
                ChatMessageId.of(id), ChatRoomId.of(10L), MemberId.of(5L),
                "hello", MessageType.TEXT, "cm-1", MessageStatus.SENT, NOW, null);
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        void 메시지를_도메인으로_반환한다() {
            ChatMessageJpaEntity entity = messageEntity(100L);
            ChatMessage domain = messageDomain(100L);
            given(jpaRepository.findById(100L)).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            Optional<ChatMessage> result = adapter.findById(100L);

            assertThat(result).isPresent();
            assertThat(result.get().getId().value()).isEqualTo(100L);
        }

        @Test
        void 존재하지_않으면_빈_Optional을_반환한다() {
            given(jpaRepository.findById(999L)).willReturn(Optional.empty());

            Optional<ChatMessage> result = adapter.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByClientId")
    class FindByClientIdTest {

        @Test
        void clientId로_메시지를_반환한다() {
            ChatMessageJpaEntity entity = messageEntity(100L);
            ChatMessage domain = messageDomain(100L);
            given(jpaRepository.findByClientId("cm-1")).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            Optional<ChatMessage> result = adapter.findByClientId("cm-1");

            assertThat(result).isPresent();
        }

        @Test
        void clientId가_없으면_빈_Optional을_반환한다() {
            given(jpaRepository.findByClientId("unknown")).willReturn(Optional.empty());

            Optional<ChatMessage> result = adapter.findByClientId("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByRoomIdBefore")
    class FindByRoomIdBeforeTest {

        @Test
        void 커서_없이_메시지_목록을_반환한다() {
            ChatMessageJpaEntity entity = messageEntity(100L);
            ChatMessage domain = messageDomain(100L);
            given(jpaRepository.findByRoom_IdAndDeletedAtIsNullOrderByIdDesc(eq(10L), any())).willReturn(List.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            List<ChatMessage> result = adapter.findByRoomIdBefore(10L, null, 20);

            assertThat(result).hasSize(1);
        }

        @Test
        void 커서와_함께_메시지_목록을_반환한다() {
            ChatMessageJpaEntity entity = messageEntity(100L);
            ChatMessage domain = messageDomain(100L);
            given(jpaRepository.findByRoom_IdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(eq(10L), eq(200L), any())).willReturn(List.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            List<ChatMessage> result = adapter.findByRoomIdBefore(10L, 200L, 20);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findBySenderIdBefore")
    class FindBySenderIdBeforeTest {

        @Test
        void 커서_없이_발신자_메시지_목록을_반환한다() {
            ChatMessageJpaEntity entity = messageEntity(100L);
            ChatMessage domain = messageDomain(100L);
            given(jpaRepository.findBySenderIdAndDeletedAtIsNullOrderByIdDesc(eq(5L), any())).willReturn(List.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            List<ChatMessage> result = adapter.findBySenderIdBefore(5L, null, 20);

            assertThat(result).hasSize(1);
        }

        @Test
        void 커서와_함께_발신자_메시지_목록을_반환한다() {
            ChatMessageJpaEntity entity = messageEntity(100L);
            ChatMessage domain = messageDomain(100L);
            given(jpaRepository.findBySenderIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(eq(5L), eq(200L), any())).willReturn(List.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            List<ChatMessage> result = adapter.findBySenderIdBefore(5L, 200L, 20);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("existsByClientId")
    class ExistsByClientIdTest {

        @Test
        void clientId가_존재하면_true를_반환한다() {
            given(jpaRepository.existsByClientId("cm-1")).willReturn(true);

            boolean result = adapter.existsByClientId("cm-1");

            assertThat(result).isTrue();
        }

        @Test
        void clientId가_없으면_false를_반환한다() {
            given(jpaRepository.existsByClientId("cm-999")).willReturn(false);

            boolean result = adapter.existsByClientId("cm-999");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTest {

        @Test
        void 새_메시지_저장_시_ID를_할당하고_동일_객체를_반환한다() {
            ChatMessage newMessage = messageDomain(null);
            ChatRoomJpaEntity roomRef = roomEntity();
            ChatMessageJpaEntity entityToSave = messageEntity(null);
            ChatMessageJpaEntity savedEntity = messageEntity(100L);

            given(entityManager.getReference(ChatRoomJpaEntity.class, 10L)).willReturn(roomRef);
            given(mapper.toJpaEntity(newMessage, roomRef)).willReturn(entityToSave);
            given(jpaRepository.save(entityToSave)).willReturn(savedEntity);

            ChatMessage result = adapter.save(newMessage);

            assertThat(result).isSameAs(newMessage);
            assertThat(result.getId().value()).isEqualTo(100L);
        }

        @Test
        void 기존_메시지_저장_시_매핑된_도메인을_반환한다() {
            ChatMessage existing = messageDomain(100L);
            ChatRoomJpaEntity roomRef = roomEntity();
            ChatMessageJpaEntity entityToSave = messageEntity(100L);
            ChatMessageJpaEntity savedEntity = messageEntity(100L);
            ChatMessage mappedBack = messageDomain(100L);

            given(entityManager.getReference(ChatRoomJpaEntity.class, 10L)).willReturn(roomRef);
            given(mapper.toJpaEntity(existing, roomRef)).willReturn(entityToSave);
            given(jpaRepository.save(entityToSave)).willReturn(savedEntity);
            given(mapper.toDomain(savedEntity)).willReturn(mappedBack);

            ChatMessage result = adapter.save(existing);

            assertThat(result).isSameAs(mappedBack);
        }
    }
}
