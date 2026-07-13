package com.bds.chat.application.member;

import com.bds.chat.application.member.service.InquiryRoomMemberService;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class InquiryRoomMemberServiceUnitExceptionTest {

    @Mock InquiryChatMemberRepository inquiryChatMemberRepository;
    @Mock Clock clock;

    @InjectMocks InquiryRoomMemberService memberService;

    private static final Long ROOM_ID = 10L;
    private static final Long MEMBER_ID = 5L;

    @Nested
    @DisplayName("채팅방 나가기 예외")
    class LeaveExceptionTest {

        @Test
        void 활성_멤버가_없으면_NOT_FOUND_예외() {
            lenient().when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
            lenient().when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
            given(inquiryChatMemberRepository.findActiveMember(ROOM_ID, MEMBER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.leave(ROOM_ID, MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("채팅방 재입장 예외")
    class RejoinExceptionTest {

        @Test
        void 멤버가_없으면_NOT_FOUND_예외() {
            lenient().when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
            lenient().when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
            given(inquiryChatMemberRepository.findByRoomIdAndMemberId(ROOM_ID, MEMBER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.rejoin(ROOM_ID, MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }
}
