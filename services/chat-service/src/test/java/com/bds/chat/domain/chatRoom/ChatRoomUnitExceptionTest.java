package com.bds.chat.domain.chatRoom;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.domain.shared.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ChatRoomUnitExceptionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("채팅방 생성 예외")
    class CreateExceptionTest {

        @Test
        void creatorId가_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> ChatRoom.create(null, ProductId.of(1L), null, ChatRoomType.INQUIRY, NOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void productId가_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> ChatRoom.create(MemberId.of(1L), null, null, ChatRoomType.INQUIRY, NOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void type이_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> ChatRoom.create(MemberId.of(1L), ProductId.of(1L), null, null, NOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void now가_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> ChatRoom.create(MemberId.of(1L), ProductId.of(1L), null, ChatRoomType.INQUIRY, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("채팅방 복원 예외")
    class RestoreExceptionTest {

        @Test
        void id가_null이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> ChatRoom.restore(null, MemberId.of(1L), ProductId.of(1L),
                    null, ChatRoomStatus.ACTIVE, ChatRoomType.INQUIRY, NOW, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("ID 할당 예외")
    class AssignIdExceptionTest {

        @Test
        void 이미_id가_있으면_CONFLICT_BusinessException이_발생한다() {
            ChatRoom room = ChatRoom.restore(ChatRoomId.of(1L), MemberId.of(1L), ProductId.of(1L),
                    null, ChatRoomStatus.ACTIVE, ChatRoomType.INQUIRY, NOW, null);
            assertThatThrownBy(() -> room.assignId(ChatRoomId.of(2L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }

        @Test
        void assignId에_null을_전달하면_NullPointerException이_발생한다() {
            ChatRoom room = ChatRoom.create(MemberId.of(1L), ProductId.of(1L), null, ChatRoomType.INQUIRY, NOW);
            assertThatThrownBy(() -> room.assignId(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
