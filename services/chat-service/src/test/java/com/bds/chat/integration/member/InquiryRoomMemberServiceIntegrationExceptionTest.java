package com.bds.chat.integration.member;

import com.bds.chat.application.chatRoom.dto.ChatRoomResponseDto;
import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.member.service.InquiryRoomMemberService;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.config.TestContainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@Transactional
@DisplayName("문의 채팅방 멤버 서비스 통합 예외 테스트")
class InquiryRoomMemberServiceIntegrationExceptionTest {

    @Autowired private InquiryRoomMemberService memberService;
    @Autowired private ChatRoomService chatRoomService;

    private static final Long SELLER_ID = 2L;
    private static final Long BUYER_ID = 7L;
    private static final Long PRODUCT_ID = 6L;
    private static final Long UNKNOWN_ROOM_ID = 999999L;
    private static final Long UNKNOWN_MEMBER_ID = 999999L;

    private Long setupInquiryRoom() {
        chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
        ChatRoomResponseDto inquiryRoom = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);
        return inquiryRoom.roomId();
    }

    @Nested
    @DisplayName("채팅방 나가기 예외")
    class LeaveExceptionTest {

        // 멤버가 아닌 사용자가 나가기 → NOT_FOUND
        @Test
        @DisplayName("멤버가 아닌 사용자가 나가면 NOT_FOUND 예외가 발생한다")
        void 멤버가_아닌_사용자가_나가면_NOT_FOUND_예외() {
            Long roomId = setupInquiryRoom();

            assertThatThrownBy(() ->
                    memberService.leave(roomId, UNKNOWN_MEMBER_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        // 이미 나간 멤버가 재시도 → NOT_FOUND
        @Test
        @DisplayName("이미 나간 멤버가 다시 나가면 NOT_FOUND 예외가 발생한다")
        void 이미_나간_멤버가_다시_나가면_NOT_FOUND_예외() {
            Long roomId = setupInquiryRoom();
            memberService.leave(roomId, BUYER_ID);

            assertThatThrownBy(() ->
                    memberService.leave(roomId, BUYER_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("채팅방 재입장 예외")
    class RejoinExceptionTest {

        // 멤버 기록 자체가 없는 방에 재입장 → NOT_FOUND
        @Test
        @DisplayName("멤버 기록이 없는 방에 재입장하면 NOT_FOUND 예외가 발생한다")
        void 멤버_기록이_없는_방에_재입장하면_NOT_FOUND_예외() {
            assertThatThrownBy(() ->
                    memberService.rejoin(UNKNOWN_ROOM_ID, UNKNOWN_MEMBER_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }
}
