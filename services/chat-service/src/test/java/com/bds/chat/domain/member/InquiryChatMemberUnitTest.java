package com.bds.chat.domain.member;

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

@ExtendWith(MockitoExtension.class)
class InquiryChatMemberUnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final ChatRoomId ROOM_ID = ChatRoomId.of(1L);
    private static final MemberId MEMBER_ID = MemberId.of(10L);

    private InquiryChatMember activeMember() {
        return InquiryChatMember.create(ROOM_ID, MEMBER_ID, NOW);
    }

    private InquiryChatMember memberWithStatus(MemberStatus status) {
        return InquiryChatMember.restore(InquiryChatMemberId.of(1L), ROOM_ID, MEMBER_ID,
                status, null, NOW, NOW, status == MemberStatus.ACTIVE ? null : NOW);
    }

    @Nested
    @DisplayName("멤버 생성")
    class CreateTest {

        @Test
        void 멤버_생성시_ACTIVE_상태로_생성된다() {
            InquiryChatMember member = InquiryChatMember.create(ROOM_ID, MEMBER_ID, NOW);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        void 멤버_생성시_id가_null이다() {
            InquiryChatMember member = InquiryChatMember.create(ROOM_ID, MEMBER_ID, NOW);
            assertThat(member.getId()).isNull();
        }

        @Test
        void 멤버_생성시_lastReadMessageId가_null이다() {
            InquiryChatMember member = InquiryChatMember.create(ROOM_ID, MEMBER_ID, NOW);
            assertThat(member.getLastReadMessageId()).isNull();
        }
    }

    @Nested
    @DisplayName("채팅방 나가기")
    class LeaveTest {

        @Test
        void ACTIVE_멤버가_나가면_LEFT_상태가_된다() {
            InquiryChatMember member = activeMember();
            member.leave(NOW);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.LEFT);
        }

        @Test
        void ACTIVE_멤버가_나가면_deletedAt이_설정된다() {
            InquiryChatMember member = activeMember();
            member.leave(NOW);
            assertThat(member.getDeletedAt()).isEqualTo(NOW);
        }

        @Test
        void 이미_나간_멤버는_재시도해도_예외없이_무시된다() {
            InquiryChatMember member = memberWithStatus(MemberStatus.LEFT);
            member.leave(NOW.plusHours(1));
            assertThat(member.getStatus()).isEqualTo(MemberStatus.LEFT);
        }
    }

    @Nested
    @DisplayName("채팅방 재입장")
    class RejoinTest {

        @Test
        void LEFT_멤버가_재입장하면_ACTIVE_상태가_된다() {
            InquiryChatMember member = memberWithStatus(MemberStatus.LEFT);
            member.rejoin(NOW);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        void LEFT_멤버가_재입장하면_deletedAt이_null이_된다() {
            InquiryChatMember member = memberWithStatus(MemberStatus.LEFT);
            member.rejoin(NOW);
            assertThat(member.getDeletedAt()).isNull();
        }

        @Test
        void 이미_ACTIVE_멤버는_재입장해도_예외없이_무시된다() {
            InquiryChatMember member = activeMember();
            member.rejoin(NOW);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("멤버 차단")
    class BanTest {

        @Test
        void ACTIVE_멤버를_차단하면_BANNED_상태가_된다() {
            InquiryChatMember member = activeMember();
            member.ban(NOW);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.BANNED);
        }

        @Test
        void 이미_차단된_멤버는_재차단해도_예외없이_무시된다() {
            InquiryChatMember member = memberWithStatus(MemberStatus.BANNED);
            member.ban(NOW.plusHours(1));
            assertThat(member.getStatus()).isEqualTo(MemberStatus.BANNED);
        }
    }

    @Nested
    @DisplayName("읽음 상태 갱신")
    class UpdateLastReadTest {

        @Test
        void lastReadMessageId가_null일때_갱신이_성공한다() {
            InquiryChatMember member = activeMember();
            member.updateLastRead(100L, NOW);
            assertThat(member.getLastReadMessageId()).isEqualTo(100L);
        }

        @Test
        void 더_큰_messageId로_갱신하면_업데이트된다() {
            InquiryChatMember member = InquiryChatMember.restore(InquiryChatMemberId.of(1L),
                    ROOM_ID, MEMBER_ID, MemberStatus.ACTIVE, 50L, NOW, NOW, null);
            member.updateLastRead(100L, NOW);
            assertThat(member.getLastReadMessageId()).isEqualTo(100L);
        }

        @Test
        void 더_작은_messageId는_갱신하지_않는다() {
            InquiryChatMember member = InquiryChatMember.restore(InquiryChatMemberId.of(1L),
                    ROOM_ID, MEMBER_ID, MemberStatus.ACTIVE, 100L, NOW, NOW, null);
            member.updateLastRead(50L, NOW);
            assertThat(member.getLastReadMessageId()).isEqualTo(100L);
        }

        @Test
        void 동일한_messageId는_갱신하지_않는다() {
            InquiryChatMember member = InquiryChatMember.restore(InquiryChatMemberId.of(1L),
                    ROOM_ID, MEMBER_ID, MemberStatus.ACTIVE, 100L, NOW, NOW, null);
            member.updateLastRead(100L, NOW);
            assertThat(member.getLastReadMessageId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("상태 조회")
    class StatusQueryTest {

        @Test
        void ACTIVE_멤버는_isActive가_true이다() {
            assertThat(activeMember().isActive()).isTrue();
        }

        @Test
        void LEFT_멤버는_isLeft가_true이다() {
            assertThat(memberWithStatus(MemberStatus.LEFT).isLeft()).isTrue();
        }

        @Test
        void BANNED_멤버는_isBanned가_true이다() {
            assertThat(memberWithStatus(MemberStatus.BANNED).isBanned()).isTrue();
        }

        @Test
        void 올바른_roomId이면_belongsTo가_true이다() {
            assertThat(activeMember().belongsTo(ROOM_ID)).isTrue();
        }

        @Test
        void 올바른_memberId이면_is가_true이다() {
            assertThat(activeMember().is(MEMBER_ID)).isTrue();
        }
    }
}
