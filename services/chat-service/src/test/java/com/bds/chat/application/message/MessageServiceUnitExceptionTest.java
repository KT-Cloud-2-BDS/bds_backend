package com.bds.chat.application.message;

import com.bds.chat.application.message.dto.MessageSendRequestDto;
import com.bds.chat.application.message.service.MessageService;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.blackList.FundingChatBlacklistRepository;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.ChatMessageRepository;
import com.bds.chat.domain.message.MessageStatus;
import com.bds.chat.domain.message.MessageType;
import com.bds.chat.domain.shared.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MessageServiceUnitExceptionTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock InquiryChatMemberRepository memberRepository;
    @Mock FundingChatBlacklistRepository blacklistRepository;
    @Mock Clock clock;

    @InjectMocks MessageService messageService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final Long ROOM_ID = 10L;
    private static final Long MSG_ID = 100L;
    private static final Long SENDER_ID = 5L;
    private static final Long CREATOR_ID = 2L;
    private static final Long PRODUCT_ID = 1L;

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

    private ChatMessage deletedMessage() {
        return ChatMessage.restore(ChatMessageId.of(MSG_ID), ChatRoomId.of(ROOM_ID), MemberId.of(SENDER_ID),
                "hello", MessageType.TEXT, "cm-1", MessageStatus.DELETED, NOW, NOW);
    }

    @Nested
    @DisplayName("메시지 생성 예외")
    class CreateExceptionTest {

        @Test
        void 방이_없으면_NOT_FOUND_예외() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "TEXT", null);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.create(request, SENDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void 유효하지_않은_type이면_INVALID_INPUT_예외() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "INVALID_TYPE", null);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));

            assertThatThrownBy(() -> messageService.create(request, SENDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        void INQUIRY_방에서_비멤버가_전송하면_FORBIDDEN_예외() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "TEXT", null);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));
            given(memberRepository.findActiveMember(ROOM_ID, SENDER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.create(request, SENDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        void FUNDING_방에서_블랙리스트_유저가_전송하면_FORBIDDEN_예외() {
            MessageSendRequestDto request = new MessageSendRequestDto(ROOM_ID, "hello", "TEXT", null);

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(blacklistRepository.isBlacklisted(ROOM_ID, SENDER_ID)).willReturn(true);

            assertThatThrownBy(() -> messageService.create(request, SENDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("메시지 삭제 예외")
    class DeleteExceptionTest {

        @Test
        void 메시지가_없으면_NOT_FOUND_예외() {
            given(chatMessageRepository.findById(MSG_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.delete(MSG_ID, SENDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void 이미_삭제된_메시지면_NOT_FOUND_예외() {
            given(chatMessageRepository.findById(MSG_ID)).willReturn(Optional.of(deletedMessage()));

            assertThatThrownBy(() -> messageService.delete(MSG_ID, SENDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void 방이_없으면_NOT_FOUND_예외() {
            given(chatMessageRepository.findById(MSG_ID)).willReturn(Optional.of(sentMessage()));
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.delete(MSG_ID, SENDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void sender도_creator도_아니면_FORBIDDEN_예외() {
            Long outsiderId = 99L;

            given(chatMessageRepository.findById(MSG_ID)).willReturn(Optional.of(sentMessage()));
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));

            assertThatThrownBy(() -> messageService.delete(MSG_ID, outsiderId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("문의방 메시지 조회 예외")
    class GetInquiryMessagesExceptionTest {

        @Test
        void 방이_없으면_NOT_FOUND_예외() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.getInquiryMessages(ROOM_ID, SENDER_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void INQUIRY_방인데_memberId가_없으면_NOT_FOUND_예외() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));

            assertThatThrownBy(() -> messageService.getInquiryMessages(ROOM_ID, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void INQUIRY_방에서_비멤버면_FORBIDDEN_예외() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));
            given(memberRepository.findActiveMember(ROOM_ID, SENDER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.getInquiryMessages(ROOM_ID, SENDER_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("펀딩방 메시지 조회 예외")
    class GetFundingMessagesExceptionTest {

        @Test
        void 방이_없으면_NOT_FOUND_예외() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.getFundingMessages(ROOM_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void INQUIRY_방이면_NOT_FOUND_예외() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));

            assertThatThrownBy(() -> messageService.getFundingMessages(ROOM_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }
}
