package com.bds.chat.application.member;

import com.bds.chat.application.member.dto.InquiryMemberLeaveResponseDto;
import com.bds.chat.application.member.service.InquiryRoomMemberService;
import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.member.MemberStatus;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.InquiryChatMemberId;
import com.bds.chat.domain.shared.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InquiryRoomMemberServiceUnitTest {

    @Mock InquiryChatMemberRepository inquiryChatMemberRepository;
    @Mock Clock clock;

    @InjectMocks InquiryRoomMemberService memberService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final Long ROOM_ID = 10L;
    private static final Long MEMBER_ID = 5L;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2026-01-01T00:00:00Z"));
        given(clock.getZone()).willReturn(ZoneOffset.UTC);
    }

    private InquiryChatMember activeMember() {
        return InquiryChatMember.restore(InquiryChatMemberId.of(1L), ChatRoomId.of(ROOM_ID),
                MemberId.of(MEMBER_ID), MemberStatus.ACTIVE, null, NOW, NOW, null);
    }

    private InquiryChatMember leftMember() {
        return InquiryChatMember.restore(InquiryChatMemberId.of(1L), ChatRoomId.of(ROOM_ID),
                MemberId.of(MEMBER_ID), MemberStatus.LEFT, null, NOW, NOW, NOW);
    }

    @Nested
    @DisplayName("채팅방 나가기")
    class LeaveTest {

        @Test
        void 멤버가_나가면_LEFT_상태의_응답을_반환한다() {
            InquiryChatMember left = leftMember();

            given(inquiryChatMemberRepository.findActiveMember(ROOM_ID, MEMBER_ID)).willReturn(Optional.of(activeMember()));
            given(inquiryChatMemberRepository.save(any())).willReturn(left);

            InquiryMemberLeaveResponseDto result = memberService.leave(ROOM_ID, MEMBER_ID);

            assertThat(result.isLeft()).isTrue();
            assertThat(result.roomId()).isEqualTo(ROOM_ID);
            assertThat(result.memberId()).isEqualTo(MEMBER_ID);
            assertThat(result.leftAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("채팅방 재입장")
    class RejoinTest {

        @Test
        void 나간_멤버가_재입장하면_예외없이_성공한다() {
            given(inquiryChatMemberRepository.findByRoomIdAndMemberId(ROOM_ID, MEMBER_ID))
                    .willReturn(Optional.of(leftMember()));
            given(inquiryChatMemberRepository.save(any())).willReturn(activeMember());

            assertThatCode(() -> memberService.rejoin(ROOM_ID, MEMBER_ID)).doesNotThrowAnyException();
        }
    }
}
