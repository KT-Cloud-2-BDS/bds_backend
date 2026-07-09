package com.bds.chat.domain.blackList;

import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.FundingChatBlacklistId;
import com.bds.chat.domain.shared.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FundingChatBlacklistUnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final ChatRoomId ROOM_ID = ChatRoomId.of(1L);
    private static final MemberId MEMBER_ID = MemberId.of(5L);

    @Nested
    @DisplayName("블랙리스트 생성")
    class CreateTest {

        @Test
        void 생성시_ACTIVE_상태로_생성된다() {
            FundingChatBlacklist bl = FundingChatBlacklist.create(ROOM_ID, MEMBER_ID, "abuse", NOW);
            assertThat(bl.getStatus()).isEqualTo(BlacklistStatus.ACTIVE);
        }

        @Test
        void 생성시_id가_null이다() {
            FundingChatBlacklist bl = FundingChatBlacklist.create(ROOM_ID, MEMBER_ID, "abuse", NOW);
            assertThat(bl.getId()).isNull();
        }

        @Test
        void 생성시_모든_필드가_올바르게_설정된다() {
            FundingChatBlacklist bl = FundingChatBlacklist.create(ROOM_ID, MEMBER_ID, "spam", NOW);
            assertThat(bl.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(bl.getMemberId()).isEqualTo(MEMBER_ID);
            assertThat(bl.getReason()).isEqualTo("spam");
            assertThat(bl.getBannedAt()).isEqualTo(NOW);
            assertThat(bl.getDeletedAt()).isNull();
        }

        @Test
        void reason이_null이어도_생성된다() {
            FundingChatBlacklist bl = FundingChatBlacklist.create(ROOM_ID, MEMBER_ID, null, NOW);
            assertThat(bl.getReason()).isNull();
            assertThat(bl.getStatus()).isEqualTo(BlacklistStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("ID 할당")
    class AssignIdTest {

        @Test
        void id_할당이_성공한다() {
            FundingChatBlacklist bl = FundingChatBlacklist.create(ROOM_ID, MEMBER_ID, null, NOW);
            FundingChatBlacklistId id = FundingChatBlacklistId.of(7L);
            bl.assignId(id);
            assertThat(bl.getId()).isEqualTo(id);
        }
    }

    @Nested
    @DisplayName("차단 해제")
    class ReleaseTest {

        @Test
        void ACTIVE_차단을_해제하면_RELEASED_상태가_된다() {
            FundingChatBlacklist bl = FundingChatBlacklist.create(ROOM_ID, MEMBER_ID, null, NOW);
            bl.release(NOW);
            assertThat(bl.getStatus()).isEqualTo(BlacklistStatus.RELEASED);
        }

        @Test
        void ACTIVE_차단을_해제하면_deletedAt이_설정된다() {
            FundingChatBlacklist bl = FundingChatBlacklist.create(ROOM_ID, MEMBER_ID, null, NOW);
            bl.release(NOW);
            assertThat(bl.getDeletedAt()).isEqualTo(NOW);
        }

        @Test
        void 이미_해제된_차단을_재시도해도_예외없이_무시된다() {
            FundingChatBlacklist bl = FundingChatBlacklist.restore(FundingChatBlacklistId.of(1L),
                    ROOM_ID, MEMBER_ID, null, BlacklistStatus.RELEASED, NOW, NOW);
            bl.release(NOW.plusHours(1));
            assertThat(bl.getStatus()).isEqualTo(BlacklistStatus.RELEASED);
            assertThat(bl.getDeletedAt()).isEqualTo(NOW);
        }
    }
}
