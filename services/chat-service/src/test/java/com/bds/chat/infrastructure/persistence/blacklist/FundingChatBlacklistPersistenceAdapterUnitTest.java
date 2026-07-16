package com.bds.chat.infrastructure.persistence.blacklist;

import com.bds.chat.domain.blackList.BlacklistStatus;
import com.bds.chat.domain.blackList.FundingChatBlacklist;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.FundingChatBlacklistId;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FundingChatBlacklistPersistenceAdapterUnitTest {

    @Mock FundingChatBlacklistJpaRepository jpaRepository;
    @Mock FundingChatBlacklistMapper mapper;
    @Mock EntityManager entityManager;

    @InjectMocks FundingChatBlacklistPersistenceAdapter adapter;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
    }

    private ChatRoomJpaEntity roomEntity() {
        return ChatRoomJpaEntity.builder()
                .id(10L).creatorId(2L).productId(1L)
                .status(ChatRoomStatus.ACTIVE).type(ChatRoomType.FUNDING).createdAt(NOW)
                .build();
    }

    private FundingChatBlacklistJpaEntity blacklistEntity(Long id) {
        return FundingChatBlacklistJpaEntity.builder()
                .id(id).room(roomEntity()).memberId(7L)
                .reason("spam").status(BlacklistStatus.ACTIVE).bannedAt(NOW).deletedAt(null)
                .build();
    }

    private FundingChatBlacklist blacklistDomain(Long id) {
        if (id == null) return FundingChatBlacklist.create(ChatRoomId.of(10L), MemberId.of(7L), "spam", NOW);
        return FundingChatBlacklist.restore(
                FundingChatBlacklistId.of(id), ChatRoomId.of(10L), MemberId.of(7L),
                "spam", BlacklistStatus.ACTIVE, NOW, null);
    }

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlacklistedTest {

        @Test
        void 블랙리스트에_있으면_true를_반환한다() {
            given(jpaRepository.existsByRoom_IdAndMemberIdAndStatus(10L, 7L, BlacklistStatus.ACTIVE)).willReturn(true);

            boolean result = adapter.isBlacklisted(10L, 7L);

            assertThat(result).isTrue();
        }

        @Test
        void 블랙리스트에_없으면_false를_반환한다() {
            given(jpaRepository.existsByRoom_IdAndMemberIdAndStatus(10L, 99L, BlacklistStatus.ACTIVE)).willReturn(false);

            boolean result = adapter.isBlacklisted(10L, 99L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findBlacklist")
    class FindBlacklistTest {

        @Test
        void 블랙리스트_항목을_도메인으로_반환한다() {
            FundingChatBlacklistJpaEntity entity = blacklistEntity(1L);
            FundingChatBlacklist domain = blacklistDomain(1L);
            given(jpaRepository.findByRoom_IdAndMemberIdAndStatus(10L, 7L, BlacklistStatus.ACTIVE))
                    .willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            Optional<FundingChatBlacklist> result = adapter.findBlacklist(10L, 7L);

            assertThat(result).isPresent();
            assertThat(result.get().getMemberId().value()).isEqualTo(7L);
        }

        @Test
        void 블랙리스트가_없으면_빈_Optional을_반환한다() {
            given(jpaRepository.findByRoom_IdAndMemberIdAndStatus(10L, 99L, BlacklistStatus.ACTIVE))
                    .willReturn(Optional.empty());

            Optional<FundingChatBlacklist> result = adapter.findBlacklist(10L, 99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTest {

        @Test
        void 새_블랙리스트_저장_시_ID를_할당하고_동일_객체를_반환한다() {
            FundingChatBlacklist newBlacklist = blacklistDomain(null);
            ChatRoomJpaEntity roomRef = roomEntity();
            FundingChatBlacklistJpaEntity entityToSave = blacklistEntity(null);
            FundingChatBlacklistJpaEntity savedEntity = blacklistEntity(1L);

            given(entityManager.getReference(ChatRoomJpaEntity.class, 10L)).willReturn(roomRef);
            given(mapper.toJpaEntity(newBlacklist, roomRef)).willReturn(entityToSave);
            given(jpaRepository.save(entityToSave)).willReturn(savedEntity);

            FundingChatBlacklist result = adapter.save(newBlacklist);

            assertThat(result).isSameAs(newBlacklist);
            assertThat(result.getId().value()).isEqualTo(1L);
        }

        @Test
        void 기존_블랙리스트_저장_시_매핑된_도메인을_반환한다() {
            FundingChatBlacklist existing = blacklistDomain(1L);
            ChatRoomJpaEntity roomRef = roomEntity();
            FundingChatBlacklistJpaEntity entityToSave = blacklistEntity(1L);
            FundingChatBlacklistJpaEntity savedEntity = blacklistEntity(1L);
            FundingChatBlacklist mappedBack = blacklistDomain(1L);

            given(entityManager.getReference(ChatRoomJpaEntity.class, 10L)).willReturn(roomRef);
            given(mapper.toJpaEntity(existing, roomRef)).willReturn(entityToSave);
            given(jpaRepository.save(entityToSave)).willReturn(savedEntity);
            given(mapper.toDomain(savedEntity)).willReturn(mappedBack);

            FundingChatBlacklist result = adapter.save(existing);

            assertThat(result).isSameAs(mappedBack);
        }
    }
}
