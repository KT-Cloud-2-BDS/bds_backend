package com.bds.chat.application.chatRoom;

import com.bds.chat.application.chatRoom.service.ChatRoomAccessService;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.domain.shared.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatRoomAccessServiceUnitTest {

    @Mock ChatRoomRepository chatRoomRepository;
    @Mock InquiryChatMemberRepository inquiryChatMemberRepository;

    @InjectMocks ChatRoomAccessService chatRoomAccessService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final Long ROOM_ID = 10L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long MEMBER_ID = 5L;

    private ChatRoom fundingRoom() {
        return ChatRoom.restore(ChatRoomId.of(ROOM_ID), MemberId.of(2L), ProductId.of(PRODUCT_ID),
                null, ChatRoomStatus.ACTIVE, ChatRoomType.FUNDING, NOW, null);
    }

    private ChatRoom inquiryRoom() {
        return ChatRoom.restore(ChatRoomId.of(ROOM_ID), MemberId.of(2L), ProductId.of(PRODUCT_ID),
                null, ChatRoomStatus.ACTIVE, ChatRoomType.INQUIRY, NOW, null);
    }

    @Nested
    @DisplayName("구독 가능 여부 확인")
    class CanSubscribeTest {

        @Test
        void 방이_없으면_false를_반환한다() {
            boolean result = chatRoomAccessService.canSubscribe(ROOM_ID, Optional.of(String.valueOf(MEMBER_ID)));

            assertThat(result).isFalse();
        }

        @Test
        void FUNDING_방이면_항상_true를_반환한다() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));

            boolean result = chatRoomAccessService.canSubscribe(ROOM_ID, Optional.empty());

            assertThat(result).isTrue();
        }

        @Test
        void INQUIRY_방에서_빈_userId이면_false를_반환한다() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));

            boolean result = chatRoomAccessService.canSubscribe(ROOM_ID, Optional.empty());

            assertThat(result).isFalse();
        }

        @Test
        void INQUIRY_방에서_활성_멤버이면_true를_반환한다() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));
            given(inquiryChatMemberRepository.existsActiveMember(ROOM_ID, MEMBER_ID)).willReturn(true);

            boolean result = chatRoomAccessService.canSubscribe(ROOM_ID, Optional.of(String.valueOf(MEMBER_ID)));

            assertThat(result).isTrue();
        }

        @Test
        void INQUIRY_방에서_비멤버이면_false를_반환한다() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));
            given(inquiryChatMemberRepository.existsActiveMember(ROOM_ID, MEMBER_ID)).willReturn(false);

            boolean result = chatRoomAccessService.canSubscribe(ROOM_ID, Optional.of(String.valueOf(MEMBER_ID)));

            assertThat(result).isFalse();
        }

        @Test
        void INQUIRY_방에서_숫자가_아닌_userId이면_false를_반환한다() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(inquiryRoom()));

            boolean result = chatRoomAccessService.canSubscribe(ROOM_ID, Optional.of("not-a-number"));

            assertThat(result).isFalse();
        }
    }
}
