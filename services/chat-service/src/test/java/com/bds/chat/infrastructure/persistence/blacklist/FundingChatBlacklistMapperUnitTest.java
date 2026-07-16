package com.bds.chat.infrastructure.persistence.blacklist;

import com.bds.chat.domain.blackList.BlacklistStatus;
import com.bds.chat.domain.blackList.FundingChatBlacklist;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.FundingChatBlacklistId;
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

class FundingChatBlacklistMapperUnitTest {

    private final FundingChatBlacklistMapper mapper = new FundingChatBlacklistMapper();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    private ChatRoomJpaEntity roomEntity() {
        return ChatRoomJpaEntity.builder()
                .id(10L).creatorId(2L).productId(1L)
                .status(ChatRoomStatus.ACTIVE).type(ChatRoomType.FUNDING).createdAt(NOW)
                .build();
    }

    private FundingChatBlacklistJpaEntity blacklistEntity() {
        return FundingChatBlacklistJpaEntity.builder()
                .id(1L).room(roomEntity()).memberId(7L)
                .reason("spam").status(BlacklistStatus.ACTIVE).bannedAt(NOW).deletedAt(null)
                .build();
    }

    @Nested
    @DisplayName("엔티티 → 도메인 변환")
    class ToDomainTest {

        @Test
        void 모든_필드를_올바르게_매핑한다() {
            FundingChatBlacklist result = mapper.toDomain(blacklistEntity());

            assertThat(result.getId().value()).isEqualTo(1L);
            assertThat(result.getRoomId().value()).isEqualTo(10L);
            assertThat(result.getMemberId().value()).isEqualTo(7L);
            assertThat(result.getReason()).isEqualTo("spam");
            assertThat(result.getStatus()).isEqualTo(BlacklistStatus.ACTIVE);
            assertThat(result.getBannedAt()).isEqualTo(NOW);
            assertThat(result.getDeletedAt()).isNull();
        }

        @Test
        void 엔티티_ID가_0이면_INVALID_INPUT_예외() {
            FundingChatBlacklistJpaEntity entity = FundingChatBlacklistJpaEntity.builder()
                    .id(0L).room(roomEntity()).memberId(7L)
                    .reason("spam").status(BlacklistStatus.ACTIVE).bannedAt(NOW).deletedAt(null)
                    .build();

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
            FundingChatBlacklist domain = FundingChatBlacklist.create(
                    ChatRoomId.of(10L), MemberId.of(7L), "spam", NOW);
            ChatRoomJpaEntity roomRef = roomEntity();

            FundingChatBlacklistJpaEntity result = mapper.toJpaEntity(domain, roomRef);

            assertThat(result.getId()).isNull();
            assertThat(result.getRoom().getId()).isEqualTo(10L);
            assertThat(result.getMemberId()).isEqualTo(7L);
            assertThat(result.getReason()).isEqualTo("spam");
            assertThat(result.getStatus()).isEqualTo(BlacklistStatus.ACTIVE);
        }

        @Test
        void ID가_있는_도메인도_올바르게_매핑한다() {
            FundingChatBlacklist domain = FundingChatBlacklist.restore(
                    FundingChatBlacklistId.of(1L), ChatRoomId.of(10L), MemberId.of(7L),
                    "spam", BlacklistStatus.ACTIVE, NOW, null);
            ChatRoomJpaEntity roomRef = roomEntity();

            FundingChatBlacklistJpaEntity result = mapper.toJpaEntity(domain, roomRef);

            assertThat(result.getId()).isEqualTo(1L);
        }
    }
}
