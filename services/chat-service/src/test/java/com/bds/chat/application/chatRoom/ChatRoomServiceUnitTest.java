package com.bds.chat.application.chatRoom;

import com.bds.chat.application.chatRoom.dto.*;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.member.MemberStatus;
import com.bds.chat.domain.message.ChatMessageRepository;
import com.bds.chat.domain.message.LatestWithUnread;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceUnitTest {

    @Mock ChatRoomRepository chatRoomRepository;
    @Mock InquiryChatMemberRepository memberRepository;
    @Mock ChatMessageRepository chatMessageRepository;
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
    @DisplayName("문의방 생성")
    class CreateInquiryRoomTest {

        @Test
        void 기존_방이_없으면_새_문의방을_생성한다() {
            ChatRoom funding = fundingRoom(SELLER_ID);
            ChatRoom saved = inquiryRoom(BUYER_ID);
            InquiryChatMember savedMember = activeMember(1L, ROOM_ID, BUYER_ID);

            given(chatRoomRepository.findInquiryRoomByProductAndBuyer(PRODUCT_ID, BUYER_ID)).willReturn(Optional.empty());
            given(chatRoomRepository.findFundingRoomByProduct(PRODUCT_ID)).willReturn(Optional.of(funding));
            given(chatRoomRepository.save(any())).willReturn(saved);
            given(memberRepository.save(any())).willReturn(savedMember);

            ChatRoomResponseDto result = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);

            assertThat(result.roomId()).isEqualTo(ROOM_ID);
            assertThat(result.type()).isEqualTo("INQUIRY");
        }

        @Test
        void 기존_방이_있으면_멤버들을_rejoin시킨다() {
            ChatRoom existing = inquiryRoom(SELLER_ID);
            InquiryChatMember buyerMember = activeMember(1L, ROOM_ID, BUYER_ID);
            InquiryChatMember sellerMember = activeMember(2L, ROOM_ID, SELLER_ID);

            given(chatRoomRepository.findInquiryRoomByProductAndBuyer(PRODUCT_ID, BUYER_ID))
                    .willReturn(Optional.of(existing));
            given(memberRepository.findAllByRoomId(ROOM_ID)).willReturn(List.of(buyerMember, sellerMember));
            given(memberRepository.save(any())).willReturn(buyerMember);

            ChatRoomResponseDto result = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);

            assertThat(result.roomId()).isEqualTo(ROOM_ID);
            assertThat(result.participants()).contains(BUYER_ID);
        }
    }

    @Nested
    @DisplayName("펀딩방 생성")
    class CreateFundingRoomTest {

        @Test
        void 펀딩방을_성공적으로_생성한다() {
            FundingRoomCreateRequestDto request = new FundingRoomCreateRequestDto(SELLER_ID);
            ChatRoom saved = fundingRoom(SELLER_ID);

            given(chatRoomRepository.findFundingRoomByProduct(PRODUCT_ID)).willReturn(Optional.empty());
            given(chatRoomRepository.save(any())).willReturn(saved);

            ChatRoomResponseDto result = chatRoomService.createFundingRoom(PRODUCT_ID, request);

            assertThat(result.roomId()).isEqualTo(ROOM_ID);
            assertThat(result.type()).isEqualTo("FUNDING");
        }
    }

    @Nested
    @DisplayName("채팅방 삭제")
    class DeleteChatRoomTest {

        @Test
        void creator가_방을_삭제하면_성공한다() {
            ChatRoom room = inquiryRoom(BUYER_ID);

            given(chatRoomRepository.findActiveByIdForUpdate(ROOM_ID)).willReturn(Optional.of(room));
            given(chatRoomRepository.save(any())).willReturn(room);

            ChatRoomDeleteResponseDto result = chatRoomService.delete(ROOM_ID, BUYER_ID);

            assertThat(result.isDeleted()).isTrue();
            assertThat(result.roomId()).isEqualTo(ROOM_ID);
        }
    }

    @Nested
    @DisplayName("문의방 상세 조회")
    class GetInquiryChatRoomByIdTest {

        @Test
        void 멤버가_문의방_상세를_조회할_수_있다() {
            ChatRoom room = inquiryRoom(SELLER_ID);
            InquiryChatMember member = activeMember(1L, ROOM_ID, BUYER_ID);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(room));
            given(memberRepository.findActiveMembers(ROOM_ID)).willReturn(List.of(member));
            given(chatMessageRepository.findLatestWithUnread(ROOM_ID, null))
                    .willReturn(new LatestWithUnread(null, 0L));

            InquiryChatRoomDetailResponseDto result = chatRoomService.getInquiryChatRoomById(ROOM_ID, BUYER_ID);

            assertThat(result.roomId()).isEqualTo(ROOM_ID);
            assertThat(result.type()).isEqualTo("INQUIRY");
            assertThat(result.participants()).contains(BUYER_ID);
        }
    }

    @Nested
    @DisplayName("펀딩방 상세 조회")
    class GetFundingChatRoomByIdTest {

        @Test
        void 펀딩방_상세를_조회할_수_있다() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom(SELLER_ID)));

            ChatRoomResponseDto result = chatRoomService.getFundingChatRoomById(ROOM_ID);

            assertThat(result.roomId()).isEqualTo(ROOM_ID);
            assertThat(result.type()).isEqualTo("FUNDING");
        }
    }

    @Nested
    @DisplayName("내 문의방 목록 조회")
    class GetMyInquiryRoomsTest {

        @Test
        void 멤버십이_없으면_빈_목록을_반환한다() {
            given(memberRepository.findByMemberId(BUYER_ID)).willReturn(List.of());

            InquiryRoomListResponseDto result = chatRoomService.getMyInquiryRooms(BUYER_ID, null, 10);

            assertThat(result.rooms()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        void 멤버십이_있으면_방_목록을_반환한다() {
            InquiryChatMember membership = activeMember(1L, ROOM_ID, BUYER_ID);
            ChatRoom room = inquiryRoom(SELLER_ID);

            given(memberRepository.findByMemberId(BUYER_ID)).willReturn(List.of(membership));
            given(chatRoomRepository.findActiveByIds(List.of(ROOM_ID), null, 11)).willReturn(List.of(room));
            given(memberRepository.findActiveMembersByRoomIds(List.of(ROOM_ID))).willReturn(List.of(membership));
            given(chatMessageRepository.findLatestWithUnreadBatch(eq(List.of(ROOM_ID)), anyMap()))
                    .willReturn(Map.of());

            InquiryRoomListResponseDto result = chatRoomService.getMyInquiryRooms(BUYER_ID, null, 10);

            assertThat(result.rooms()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
        }
    }
}
