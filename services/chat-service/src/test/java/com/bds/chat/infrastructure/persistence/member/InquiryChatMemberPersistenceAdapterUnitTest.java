package com.bds.chat.infrastructure.persistence.member;

import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.MemberStatus;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.InquiryChatMemberId;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InquiryChatMemberPersistenceAdapterUnitTest {

    @Mock InquiryChatMemberJpaRepository jpaRepository;
    @Mock InquiryChatMemberMapper mapper;
    @Mock EntityManager entityManager;

    @InjectMocks InquiryChatMemberPersistenceAdapter adapter;

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

    private InquiryChatMemberJpaEntity memberEntity() {
        return InquiryChatMemberJpaEntity.builder()
                .id(1L).room(roomEntity()).memberId(5L)
                .status(MemberStatus.ACTIVE).lastReadMessageId(null)
                .joinedAt(NOW).updatedAt(NOW).deletedAt(null)
                .build();
    }

    private InquiryChatMember memberDomain(Long id) {
        if (id == null) return InquiryChatMember.create(ChatRoomId.of(10L), MemberId.of(5L), NOW);
        return InquiryChatMember.restore(
                InquiryChatMemberId.of(id), ChatRoomId.of(10L), MemberId.of(5L),
                MemberStatus.ACTIVE, null, NOW, NOW, null);
    }

    @Nested
    @DisplayName("findActiveMember")
    class FindActiveMemberTest {

        @Test
        void 활성_멤버를_도메인으로_반환한다() {
            InquiryChatMemberJpaEntity entity = memberEntity();
            InquiryChatMember domain = memberDomain(1L);
            given(jpaRepository.findByRoom_IdAndMemberIdAndDeletedAtIsNull(10L, 5L)).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            Optional<InquiryChatMember> result = adapter.findActiveMember(10L, 5L);

            assertThat(result).isPresent();
            assertThat(result.get().getMemberId().value()).isEqualTo(5L);
        }

        @Test
        void 활성_멤버가_없으면_빈_Optional을_반환한다() {
            given(jpaRepository.findByRoom_IdAndMemberIdAndDeletedAtIsNull(10L, 99L)).willReturn(Optional.empty());

            Optional<InquiryChatMember> result = adapter.findActiveMember(10L, 99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsActiveMember")
    class ExistsActiveMemberTest {

        @Test
        void 활성_멤버가_존재하면_true를_반환한다() {
            given(jpaRepository.existsByRoom_IdAndMemberIdAndDeletedAtIsNull(10L, 5L)).willReturn(true);

            boolean result = adapter.existsActiveMember(10L, 5L);

            assertThat(result).isTrue();
        }

        @Test
        void 활성_멤버가_없으면_false를_반환한다() {
            given(jpaRepository.existsByRoom_IdAndMemberIdAndDeletedAtIsNull(10L, 99L)).willReturn(false);

            boolean result = adapter.existsActiveMember(10L, 99L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findAllByRoomId")
    class FindAllByRoomIdTest {

        @Test
        void 방의_모든_멤버를_도메인_목록으로_반환한다() {
            InquiryChatMemberJpaEntity entity = memberEntity();
            InquiryChatMember domain = memberDomain(1L);
            given(jpaRepository.findByRoom_Id(10L)).willReturn(List.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            List<InquiryChatMember> result = adapter.findAllByRoomId(10L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTest {

        @Test
        void 새_멤버_저장_시_ID를_할당하고_동일_객체를_반환한다() {
            InquiryChatMember newMember = memberDomain(null);
            ChatRoomJpaEntity roomRef = roomEntity();
            InquiryChatMemberJpaEntity entityToSave = memberEntity();
            InquiryChatMemberJpaEntity savedEntity = memberEntity();

            given(entityManager.getReference(ChatRoomJpaEntity.class, 10L)).willReturn(roomRef);
            given(mapper.toJpaEntity(newMember, roomRef)).willReturn(entityToSave);
            given(jpaRepository.save(entityToSave)).willReturn(savedEntity);

            InquiryChatMember result = adapter.save(newMember);

            assertThat(result).isSameAs(newMember);
            assertThat(result.getId().value()).isEqualTo(1L);
        }

        @Test
        void 기존_멤버_저장_시_매핑된_도메인을_반환한다() {
            InquiryChatMember existing = memberDomain(1L);
            ChatRoomJpaEntity roomRef = roomEntity();
            InquiryChatMemberJpaEntity entityToSave = memberEntity();
            InquiryChatMemberJpaEntity savedEntity = memberEntity();
            InquiryChatMember mappedBack = memberDomain(1L);

            given(entityManager.getReference(ChatRoomJpaEntity.class, 10L)).willReturn(roomRef);
            given(mapper.toJpaEntity(existing, roomRef)).willReturn(entityToSave);
            given(jpaRepository.save(entityToSave)).willReturn(savedEntity);
            given(mapper.toDomain(savedEntity)).willReturn(mappedBack);

            InquiryChatMember result = adapter.save(existing);

            assertThat(result).isSameAs(mappedBack);
        }
    }
}
