package com.bds.chat.application.chatRoom;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.member.MemberStatus;
import com.bds.chat.domain.shared.*;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceUnitExceptionTest {

    @Mock ChatRoomRepository chatRoomRepository;
    @Mock InquiryChatMemberRepository memberRepository;
    @Mock Clock clock;

    @InjectMocks ChatRoomService chatRoomService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final Long ROOM_ID = 10L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long BUYER_ID = 5L;
    private static final Long SELLER_ID = 2L;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private ChatRoom inquiryRoom(Long creatorId) {
        return ChatRoom.restore(ChatRoomId.of(ROOM_ID), MemberId.of(creatorId), ProductId.of(PRODUCT_ID),
                null, ChatRoomStatus.ACTIVE, ChatRoomType.INQUIRY, NOW, null);
    }

    private ChatRoom fundingRoom(Long creatorId) {
        return ChatRoom.restore(ChatRoomId.of(ROOM_ID), MemberId.of(creatorId), ProductId.of(PRODUCT_ID),
                null, ChatRoomStatus.ACTIVE, ChatRoomType.FUNDING, NOW, null);
    }

    private InquiryChatMember activeMember(Long id, Long roomId, Long memberId) {
        return InquiryChatMember.restore(InquiryChatMemberId.of(id), ChatRoomId.of(roomId),
                MemberId.of(memberId), MemberStatus.ACTIVE, null, NOW, NOW, null);
    }

    @Nested
    @DisplayName("문의방 생성 예외")
    class CreateInquiryRoomExceptionTest {

        @Test
        void 기존_방에_buyer가_없으면_NOT_FOUND_예외() {
            ChatRoom existing = inquiryRoom(SELLER_ID);
            InquiryChatMember sellerOnly = activeMember(1L, ROOM_ID, SELLER_ID);

            lenient().when(chatRoomRepository.findInquiryRoomByProductAndBuyer(PRODUCT_ID, BUYER_ID))
                    .thenReturn(Optional.of(existing));
            lenient().when(memberRepository.findAllByRoomId(ROOM_ID)).thenReturn(List.of(sellerOnly));

            assertThatThrownBy(() -> chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void 기존_방도_없고_펀딩방도_없으면_NOT_FOUND_예외() {
            lenient().when(chatRoomRepository.findInquiryRoomByProductAndBuyer(PRODUCT_ID, BUYER_ID))
                    .thenReturn(Optional.empty());
            lenient().when(chatRoomRepository.findFundingRoomByProduct(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("펀딩방 생성 예외")
    class CreateFundingRoomExceptionTest {

        @Test
        void 이미_펀딩방이_있으면_CONFLICT_예외() {
            FundingRoomCreateRequestDto request = new FundingRoomCreateRequestDto(SELLER_ID);

            lenient().when(chatRoomRepository.findFundingRoomByProduct(PRODUCT_ID))
                    .thenReturn(Optional.of(fundingRoom(SELLER_ID)));

            assertThatThrownBy(() -> chatRoomService.createFundingRoom(PRODUCT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }

    @Nested
    @DisplayName("채팅방 삭제 예외")
    class DeleteChatRoomExceptionTest {

        @Test
        void 방이_없으면_NOT_FOUND_예외() {
            lenient().when(chatRoomRepository.findActiveByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatRoomService.delete(ROOM_ID, BUYER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void creator가_아니면_FORBIDDEN_예외() {
            ChatRoom room = inquiryRoom(SELLER_ID);

            lenient().when(chatRoomRepository.findActiveByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> chatRoomService.delete(ROOM_ID, BUYER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("문의방 상세 조회 예외")
    class GetInquiryChatRoomByIdExceptionTest {

        @Test
        void memberId가_null이면_FORBIDDEN_예외() {
            assertThatThrownBy(() -> chatRoomService.getInquiryChatRoomById(ROOM_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        void 방이_없으면_NOT_FOUND_예외() {
            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatRoomService.getInquiryChatRoomById(ROOM_ID, BUYER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void FUNDING_방이면_NOT_FOUND_예외() {
            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.of(fundingRoom(SELLER_ID)));

            assertThatThrownBy(() -> chatRoomService.getInquiryChatRoomById(ROOM_ID, BUYER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void 멤버가_아니면_FORBIDDEN_예외() {
            ChatRoom room = inquiryRoom(SELLER_ID);
            InquiryChatMember sellerMember = activeMember(1L, ROOM_ID, SELLER_ID);

            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.of(room));
            lenient().when(memberRepository.findActiveMembers(ROOM_ID)).thenReturn(List.of(sellerMember));

            assertThatThrownBy(() -> chatRoomService.getInquiryChatRoomById(ROOM_ID, BUYER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("펀딩방 상세 조회 예외")
    class GetFundingChatRoomByIdExceptionTest {

        @Test
        void 방이_없으면_NOT_FOUND_예외() {
            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatRoomService.getFundingChatRoomById(ROOM_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void INQUIRY_방이면_NOT_FOUND_예외() {
            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.of(inquiryRoom(SELLER_ID)));

            assertThatThrownBy(() -> chatRoomService.getFundingChatRoomById(ROOM_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }
}
