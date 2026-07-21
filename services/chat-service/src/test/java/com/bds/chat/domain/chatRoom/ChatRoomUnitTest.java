package com.bds.chat.domain.chatRoom;

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

@ExtendWith(MockitoExtension.class)
class ChatRoomUnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final MemberId CREATOR_ID = MemberId.of(1L);
    private static final ProductId PRODUCT_ID = ProductId.of(10L);

    @Nested
    @DisplayName("채팅방 생성")
    class CreateTest {

        @Test
        void 채팅방_생성시_ACTIVE_상태로_생성된다() {
            ChatRoom room = ChatRoom.create(CREATOR_ID, PRODUCT_ID, null, ChatRoomType.INQUIRY, NOW);
            assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
        }

        @Test
        void 채팅방_생성시_id가_null이다() {
            ChatRoom room = ChatRoom.create(CREATOR_ID, PRODUCT_ID, null, ChatRoomType.INQUIRY, NOW);
            assertThat(room.getId()).isNull();
        }

        @Test
        void 채팅방_생성시_creatorId와_productId와_type이_올바르게_설정된다() {
            ChatRoom room = ChatRoom.create(CREATOR_ID, PRODUCT_ID, "title", ChatRoomType.FUNDING, NOW);
            assertThat(room.getCreatorId()).isEqualTo(CREATOR_ID);
            assertThat(room.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(room.getType()).isEqualTo(ChatRoomType.FUNDING);
            assertThat(room.getTitle()).isEqualTo("title");
            assertThat(room.getCreatedAt()).isEqualTo(NOW);
            assertThat(room.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("채팅방 복원")
    class RestoreTest {

        @Test
        void restore로_모든_필드가_올바르게_복원된다() {
            ChatRoomId id = ChatRoomId.of(1L);
            ChatRoom room = ChatRoom.restore(id, CREATOR_ID, PRODUCT_ID, "title",
                    ChatRoomStatus.ACTIVE, ChatRoomType.INQUIRY, NOW, null);
            assertThat(room.getId()).isEqualTo(id);
            assertThat(room.getCreatorId()).isEqualTo(CREATOR_ID);
            assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
            assertThat(room.getType()).isEqualTo(ChatRoomType.INQUIRY);
        }

        @Test
        void CLOSED_상태로도_복원할_수_있다() {
            LocalDateTime deletedAt = NOW.plusHours(1);
            ChatRoom room = ChatRoom.restore(ChatRoomId.of(1L), CREATOR_ID, PRODUCT_ID, null,
                    ChatRoomStatus.CLOSED, ChatRoomType.FUNDING, NOW, deletedAt);
            assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
            assertThat(room.getDeletedAt()).isEqualTo(deletedAt);
        }
    }

    @Nested
    @DisplayName("채팅방 재오픈")
    class ReopenTest {

        @Test
        void CLOSED_채팅방을_reopen하면_ACTIVE_상태가_된다() {
            ChatRoom room = ChatRoom.restore(ChatRoomId.of(1L), CREATOR_ID, PRODUCT_ID, null,
                    ChatRoomStatus.CLOSED, ChatRoomType.FUNDING, NOW, NOW);
            room.reopen();
            assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
        }

        @Test
        void CLOSED_채팅방을_reopen하면_deletedAt이_null로_초기화된다() {
            ChatRoom room = ChatRoom.restore(ChatRoomId.of(1L), CREATOR_ID, PRODUCT_ID, null,
                    ChatRoomStatus.CLOSED, ChatRoomType.FUNDING, NOW, NOW);
            room.reopen();
            assertThat(room.getDeletedAt()).isNull();
        }

        @Test
        void 이미_ACTIVE인_채팅방을_reopen해도_예외없이_무시된다() {
            ChatRoom room = ChatRoom.create(CREATOR_ID, PRODUCT_ID, null, ChatRoomType.FUNDING, NOW);
            room.reopen();
            assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("ID 할당")
    class AssignIdTest {

        @Test
        void id_할당이_성공한다() {
            ChatRoom room = ChatRoom.create(CREATOR_ID, PRODUCT_ID, null, ChatRoomType.INQUIRY, NOW);
            ChatRoomId id = ChatRoomId.of(42L);
            room.assignId(id);
            assertThat(room.getId()).isEqualTo(id);
        }
    }

    @Nested
    @DisplayName("채팅방 삭제")
    class DeleteTest {

        @Test
        void ACTIVE_채팅방_삭제시_CLOSED_상태가_된다() {
            ChatRoom room = ChatRoom.create(CREATOR_ID, PRODUCT_ID, null, ChatRoomType.FUNDING, NOW);
            room.delete(NOW);
            assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
        }

        @Test
        void ACTIVE_채팅방_삭제시_deletedAt이_설정된다() {
            ChatRoom room = ChatRoom.create(CREATOR_ID, PRODUCT_ID, null, ChatRoomType.FUNDING, NOW);
            room.delete(NOW);
            assertThat(room.getDeletedAt()).isEqualTo(NOW);
        }

        @Test
        void 이미_삭제된_채팅방은_재삭제해도_예외없이_무시된다() {
            ChatRoom room = ChatRoom.restore(ChatRoomId.of(1L), CREATOR_ID, PRODUCT_ID, null,
                    ChatRoomStatus.CLOSED, ChatRoomType.FUNDING, NOW, NOW);
            room.delete(NOW.plusMinutes(1));
            assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
            assertThat(room.getDeletedAt()).isEqualTo(NOW);
        }
    }
}
