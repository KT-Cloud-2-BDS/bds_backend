package com.bds.chat.domain.blackList;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FundingChatBlacklistUnitExceptionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final ChatRoomId ROOM_ID = ChatRoomId.of(1L);
    private static final MemberId MEMBER_ID = MemberId.of(5L);

    @Nested
    @DisplayName("생성 예외")
    class CreateExceptionTest {

        @Test
        void roomId가_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> FundingChatBlacklist.create(null, MEMBER_ID, null, NOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void memberId가_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> FundingChatBlacklist.create(ROOM_ID, null, null, NOW))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("ID 할당 예외")
    class AssignIdExceptionTest {

        @Test
        void 이미_id가_있으면_CONFLICT_BusinessException이_발생한다() {
            FundingChatBlacklist bl = FundingChatBlacklist.restore(FundingChatBlacklistId.of(1L),
                    ROOM_ID, MEMBER_ID, null, BlacklistStatus.ACTIVE, NOW, null);
            assertThatThrownBy(() -> bl.assignId(FundingChatBlacklistId.of(2L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }
}
