package com.bds.chat.application.message;

import com.bds.chat.application.message.dto.*;
import com.bds.chat.application.message.service.MessageService;
import com.bds.chat.domain.blackList.FundingChatBlacklistRepository;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.member.MemberStatus;
import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.ChatMessageRepository;
import com.bds.chat.domain.message.MessageStatus;
import com.bds.chat.domain.message.MessageType;
import com.bds.chat.domain.shared.*;
import com.bds.chat.infrastructure.messaging.DirectEventPublisher;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MessageServiceUnitTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock InquiryChatMemberRepository memberRepository;
    @Mock FundingChatBlacklistRepository blacklistRepository;
    @Mock DirectEventPublisher directEventPublisher;
    @Mock Clock clock;

    @InjectMocks MessageService messageService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final Long ROOM_ID = 10L;
    private static final Long MSG_ID = 100L;
    private static final Long SENDER_ID = 5L;
    private static final Long RECIPIENT_ID = 7L;
    private static final Long CREATOR_ID = 2L;
    private static final Long PRODUCT_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);

    }

    private ChatRoom fundingRoom() {
        return ChatRoom.restore(ChatRoomId.of(ROOM_ID), MemberId.of(CREATOR_ID), ProductId.of(PRODUCT_ID),
                null, ChatRoomStatus.ACTIVE, ChatRoomType.FUNDING, NOW, null);
    }

    private ChatRoom inquiryRoom() {
        return ChatRoom.restore(ChatRoomId.of(ROOM_ID), MemberId.of(CREATOR_ID), ProductId.of(PRODUCT_ID),
                null, ChatRoomStatus.ACTIVE, ChatRoomType.INQUIRY, NOW, null);
    }

    private ChatMessage sentMessage() {
        return ChatMessage.restore(ChatMessageId.of(MSG_ID), ChatRoomId.of(ROOM_ID), MemberId.of(SENDER_ID),
                "hello", MessageType.TEXT, "cm-1", MessageStatus.SENT, NOW, null);
    }

    private ChatMessage systemMessage() {
        return ChatMessage.restore(ChatMessageId.of(MSG_ID), ChatRoomId.of(ROOM_ID), null,
                "시스템 알림", MessageType.TEXT, "cm-sys", MessageStatus.SENT, NOW, null);
    }

    private InquiryChatMember activeMember() {
        return InquiryChatMember.restore(InquiryChatMemberId.of(1L), ChatRoomId.of(ROOM_ID),
                MemberId.of(SENDER_ID), MemberStatus.ACTIVE, null, NOW, NOW, null);
    }

    private InquiryChatMember recipientMember() {
        return InquiryChatMember.restore(InquiryChatMemberId.of(2L), ChatRoomId.of(ROOM_ID),
                MemberId.of(RECIPIENT_ID), MemberStatus.ACTIVE, null, NOW, NOW, null);
    }

    @Nested
    @DisplayName("메시지 생성")
    class CreateTest {

        @Test
        void clientId가_null이면_새_메시지를_생성한다() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "TEXT", null);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(blacklistRepository.isBlacklisted(ROOM_ID, SENDER_ID)).willReturn(false);
            given(chatMessageRepository.save(any())).willReturn(sentMessage());

            MessageResponseDto result = messageService.create(request, SENDER_ID);

            assertThat(result.messageId()).isEqualTo(MSG_ID);
            assertThat(result.roomId()).isEqualTo(ROOM_ID);
        }

        @Test
        void clientId로_기존_메시지를_찾으면_멱등적으로_반환한다() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "TEXT", "cm-1");

            given(chatMessageRepository.findByClientId("cm-1")).willReturn(Optional.of(sentMessage()));

            MessageResponseDto result = messageService.create(request, SENDER_ID);

            assertThat(result.messageId()).isEqualTo(MSG_ID);
        }

        @Test
        void clientId가_있어도_기존_메시지가_없으면_새_메시지를_생성한다() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "TEXT", "cm-new");

            given(chatMessageRepository.findByClientId("cm-new")).willReturn(Optional.empty());
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(blacklistRepository.isBlacklisted(ROOM_ID, SENDER_ID)).willReturn(false);
            given(chatMessageRepository.save(any())).willReturn(sentMessage());

            MessageResponseDto result = messageService.create(request, SENDER_ID);

            assertThat(result.messageId()).isEqualTo(MSG_ID);
        }

        @Test
        void INQUIRY_방에서_메시지_전송_시_recipient에게_이벤트가_발행된다() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "TEXT", null);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));
            given(memberRepository.findActiveMembers(ROOM_ID)).willReturn(List.of(activeMember(), recipientMember()));
            given(chatMessageRepository.save(any())).willReturn(sentMessage());

            messageService.create(request, SENDER_ID);

            verify(directEventPublisher).publish(any());
        }

        @Test
        void INQUIRY_방에서_알림_발행_실패_시_메시지_저장은_유지된다() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "TEXT", null);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));
            given(memberRepository.findActiveMembers(ROOM_ID)).willReturn(List.of(activeMember(), recipientMember()));
            given(chatMessageRepository.save(any())).willReturn(sentMessage());
            doThrow(new RuntimeException("RabbitMQ 연결 실패")).when(directEventPublisher).publish(any());

            MessageResponseDto result = messageService.create(request, SENDER_ID);

            assertThat(result.messageId()).isEqualTo(MSG_ID);
        }

        @Test
        void senderId가_null인_시스템_메시지는_MessageResponseDto의_senderId가_null이다() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "시스템 알림", "TEXT", null);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(blacklistRepository.isBlacklisted(ROOM_ID, SENDER_ID)).willReturn(false);
            given(chatMessageRepository.save(any())).willReturn(systemMessage());

            MessageResponseDto result = messageService.create(request, SENDER_ID);

            assertThat(result.senderId()).isNull();
            assertThat(result.messageId()).isEqualTo(MSG_ID);
        }

        @Test
        void FUNDING_방에서는_알림_이벤트가_발행되지_않는다() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "TEXT", null);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(blacklistRepository.isBlacklisted(ROOM_ID, SENDER_ID)).willReturn(false);
            given(chatMessageRepository.save(any())).willReturn(sentMessage());

            messageService.create(request, SENDER_ID);

            verify(directEventPublisher, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteTest {

        @Test
        void sender가_자신의_메시지를_삭제할_수_있다() {
            ChatMessage message = sentMessage();
            ChatMessage deleted = ChatMessage.restore(ChatMessageId.of(MSG_ID), ChatRoomId.of(ROOM_ID),
                    MemberId.of(SENDER_ID), "hello", MessageType.TEXT, "cm-1", MessageStatus.DELETED, NOW, NOW);

            given(chatMessageRepository.findById(MSG_ID)).willReturn(Optional.of(message));
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(chatMessageRepository.save(any())).willReturn(deleted);

            MessageDeleteResponseDto result = messageService.delete(MSG_ID, SENDER_ID);

            assertThat(result.isDeleted()).isTrue();
        }

        @Test
        void creator가_타인의_메시지를_삭제할_수_있다() {
            ChatMessage message = sentMessage();
            ChatMessage deleted = ChatMessage.restore(ChatMessageId.of(MSG_ID), ChatRoomId.of(ROOM_ID),
                    MemberId.of(SENDER_ID), "hello", MessageType.TEXT, "cm-1", MessageStatus.DELETED, NOW, NOW);

            given(chatMessageRepository.findById(MSG_ID)).willReturn(Optional.of(message));
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(chatMessageRepository.save(any())).willReturn(deleted);

            MessageDeleteResponseDto result = messageService.delete(MSG_ID, CREATOR_ID);

            assertThat(result.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("메시지 이력 조회")
    class GetHistoryTest {

        @Test
        void 내가_보낸_메시지_목록을_반환한다() {
            given(chatMessageRepository.findBySenderIdBefore(SENDER_ID, null, 21)).willReturn(List.of(sentMessage()));

            MessageListResponseDto result = messageService.getHistory(SENDER_ID, null);

            assertThat(result.messages()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("문의방 메시지 조회")
    class GetInquiryMessagesTest {

        @Test
        void INQUIRY_방_멤버는_조회시_lastRead를_갱신한다() {
            InquiryChatMember member = activeMember();

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));
            given(memberRepository.findActiveMember(ROOM_ID, SENDER_ID)).willReturn(Optional.of(member));
            given(chatMessageRepository.findByRoomIdBefore(ROOM_ID, null, 21)).willReturn(List.of(sentMessage()));
            given(memberRepository.save(any())).willReturn(member);

            MessageListResponseDto result = messageService.getInquiryMessages(ROOM_ID, SENDER_ID, null);

            assertThat(result.messages()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("펀딩방 메시지 조회")
    class GetFundingMessagesTest {

        @Test
        void 펀딩방_메시지를_조회할_수_있다() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(chatMessageRepository.findByRoomIdBefore(ROOM_ID, null, 21)).willReturn(List.of(sentMessage()));

            MessageListResponseDto result = messageService.getFundingMessages(ROOM_ID, null);

            assertThat(result.messages()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("clientId로 메시지 조회")
    class FindByClientIdTest {

        @Test
        void clientId로_기존_메시지를_반환한다() {
            given(chatMessageRepository.findByClientId("cm-1")).willReturn(Optional.of(sentMessage()));

            MessageResponseDto result = messageService.findByClientId("cm-1");

            assertThat(result.messageId()).isEqualTo(MSG_ID);
            assertThat(result.roomId()).isEqualTo(ROOM_ID);
        }
    }
}
