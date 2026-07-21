package com.bds.chat.infrastructure.persistence.message;

import com.bds.chat.common.DuplicateClientIdException;
import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.MessageType;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatMessagePersistenceAdapterUnitExceptionTest {

    @Mock ChatMessageJpaRepository jpaRepository;
    @Mock ChatMessageMapper mapper;
    @Mock EntityManager entityManager;

    @InjectMocks ChatMessagePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
    }

    @Nested
    @DisplayName("findById 실패 케이스")
    class FindByIdFailTest {

        @Test
        void 존재하지_않는_메시지는_빈_Optional을_반환한다() {
            given(jpaRepository.findById(999L)).willReturn(Optional.empty());

            Optional<ChatMessage> result = adapter.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByClientId 실패 케이스")
    class FindByClientIdFailTest {

        @Test
        void 존재하지_않는_clientId는_빈_Optional을_반환한다() {
            given(jpaRepository.findByClientId("unknown")).willReturn(Optional.empty());

            Optional<ChatMessage> result = adapter.findByClientId("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByRoomIdBefore 실패 케이스")
    class FindByRoomIdBeforeFailTest {

        @Test
        void 메시지가_없는_방은_빈_리스트를_반환한다() {
            given(jpaRepository.findByRoom_IdAndDeletedAtIsNullOrderByIdDesc(eq(99L), any())).willReturn(List.of());

            List<ChatMessage> result = adapter.findByRoomIdBefore(99L, null, 20);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findBySenderIdBefore 실패 케이스")
    class FindBySenderIdBeforeFailTest {

        @Test
        void 메시지가_없는_발신자는_빈_리스트를_반환한다() {
            given(jpaRepository.findBySenderIdAndDeletedAtIsNullOrderByIdDesc(eq(99L), any())).willReturn(List.of());

            List<ChatMessage> result = adapter.findBySenderIdBefore(99L, null, 20);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save 실패 케이스")
    class SaveFailTest {

        private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

        @Test
        void clientId_unique_violation_시_DuplicateClientIdException_발생() {
            ChatMessage message = ChatMessage.create(
                    ChatRoomId.of(10L), MemberId.of(5L), "hello", MessageType.TEXT, "cm-dup", NOW);

            given(entityManager.getReference(any(), any())).willReturn(null);
            given(mapper.toJpaEntity(any(), any())).willReturn(new ChatMessageJpaEntity());

            SQLException sqlEx = new SQLException("duplicate key", "23505");
            given(jpaRepository.save(any()))
                    .willThrow(new DataIntegrityViolationException("constraint violation", sqlEx));

            assertThatThrownBy(() -> adapter.save(message))
                    .isInstanceOf(DuplicateClientIdException.class)
                    .satisfies(e -> assertThat(((DuplicateClientIdException) e).getClientId()).isEqualTo("cm-dup"));
        }

        @Test
        void clientId가_null이면_unique_violation이어도_DataIntegrityViolationException_그대로_전파() {
            ChatMessage message = ChatMessage.create(
                    ChatRoomId.of(10L), MemberId.of(5L), "hello", MessageType.TEXT, null, NOW);

            given(entityManager.getReference(any(), any())).willReturn(null);
            given(mapper.toJpaEntity(any(), any())).willReturn(new ChatMessageJpaEntity());

            SQLException sqlEx = new SQLException("duplicate key", "23505");
            given(jpaRepository.save(any()))
                    .willThrow(new DataIntegrityViolationException("constraint violation", sqlEx));

            assertThatThrownBy(() -> adapter.save(message))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
