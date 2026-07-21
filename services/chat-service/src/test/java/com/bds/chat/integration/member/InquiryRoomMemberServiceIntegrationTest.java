package com.bds.chat.integration.member;

import com.bds.chat.application.chatRoom.dto.ChatRoomResponseDto;
import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.member.dto.InquiryMemberLeaveResponseDto;
import com.bds.chat.application.member.service.InquiryRoomMemberService;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@Transactional
@DisplayName("문의 채팅방 멤버 서비스 통합 테스트")
class InquiryRoomMemberServiceIntegrationTest {

    @Autowired private InquiryRoomMemberService memberService;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private InquiryChatMemberRepository memberRepository;

    private static final Long SELLER_ID = 2L;
    private static final Long BUYER_ID = 7L;
    private static final Long PRODUCT_ID = 6L;

    private Long setupInquiryRoom() {
        chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
        ChatRoomResponseDto inquiryRoom = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);
        return inquiryRoom.roomId();
    }

    @Nested
    @DisplayName("채팅방 나가기")
    class LeaveTest {

        // 나가기 → DTO 검증 + ACTIVE 멤버 해제 DB 검증
        @Test
        @DisplayName("멤버가 채팅방을 나가면 ACTIVE 상태가 해제된다")
        void 멤버가_채팅방을_나가면_ACTIVE_상태가_해제된다() {
            Long roomId = setupInquiryRoom();

            InquiryMemberLeaveResponseDto result = memberService.leave(roomId, BUYER_ID);

            // Step 1: 응답 검증
            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.memberId()).isEqualTo(BUYER_ID);
            assertThat(result.leftAt()).isNotNull();

            // Step 2: DB 검증 - ACTIVE 멤버로 더 이상 조회되지 않음
            assertThat(memberRepository.findActiveMember(roomId, BUYER_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("채팅방 재입장")
    class RejoinTest {

        // 나간 후 재입장 → ACTIVE 상태 복구 DB 검증
        @Test
        @DisplayName("나간 멤버가 재입장하면 ACTIVE 상태로 복구된다")
        void 나간_멤버가_재입장하면_ACTIVE_상태로_복구된다() {
            Long roomId = setupInquiryRoom();
            memberService.leave(roomId, BUYER_ID);

            memberService.rejoin(roomId, BUYER_ID);

            // DB 검증 - ACTIVE 멤버로 다시 조회됨
            assertThat(memberRepository.findActiveMember(roomId, BUYER_ID)).isPresent();
        }
    }
}
