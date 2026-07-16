package com.bds.chat.domain.member;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.InquiryChatMemberId;
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
class InquiryChatMemberUnitExceptionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final ChatRoomId ROOM_ID = ChatRoomId.of(1L);
    private static final MemberId MEMBER_ID = MemberId.of(10L);

    private InquiryChatMember memberWithStatus(MemberStatus status) {
        return InquiryChatMember.restore(InquiryChatMemberId.of(1L), ROOM_ID, MEMBER_ID,
                status, null, NOW, NOW, status == MemberStatus.ACTIVE ? null : NOW);
    }

    @Nested
    @DisplayName("나가기 예외")
    class LeaveExceptionTest {

        @Test
        void BANNED_멤버가_나가려하면_FORBIDDEN_BusinessException이_발생한다() {
            InquiryChatMember member = memberWithStatus(MemberStatus.BANNED);
            assertThatThrownBy(() -> member.leave(NOW))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("재입장 예외")
    class RejoinExceptionTest {

        @Test
        void BANNED_멤버가_재입장하려하면_FORBIDDEN_BusinessException이_발생한다() {
            InquiryChatMember member = memberWithStatus(MemberStatus.BANNED);
            assertThatThrownBy(() -> member.rejoin(NOW))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("읽음 갱신 예외")
    class UpdateLastReadExceptionTest {

        @Test
        void LEFT_멤버가_lastRead_갱신하면_FORBIDDEN_BusinessException이_발생한다() {
            InquiryChatMember member = memberWithStatus(MemberStatus.LEFT);
            assertThatThrownBy(() -> member.updateLastRead(100L, NOW))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        void BANNED_멤버가_lastRead_갱신하면_FORBIDDEN_BusinessException이_발생한다() {
            InquiryChatMember member = memberWithStatus(MemberStatus.BANNED);
            assertThatThrownBy(() -> member.updateLastRead(100L, NOW))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("ID 할당 예외")
    class AssignIdExceptionTest {

        @Test
        void 이미_id가_있으면_CONFLICT_BusinessException이_발생한다() {
            InquiryChatMember member = memberWithStatus(MemberStatus.ACTIVE);
            assertThatThrownBy(() -> member.assignId(InquiryChatMemberId.of(2L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }
}
