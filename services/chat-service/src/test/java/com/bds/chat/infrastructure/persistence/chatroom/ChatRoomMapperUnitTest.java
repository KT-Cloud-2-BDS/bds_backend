package com.bds.chat.infrastructure.persistence.chatroom;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.domain.shared.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRoomMapperUnitTest {

    private final ChatRoomMapper mapper = new ChatRoomMapper();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    private ChatRoomJpaEntity buildEntity(Long id) {
        return ChatRoomJpaEntity.builder()
                .id(id)
                .creatorId(5L)
                .productId(1L)
                .title(null)
                .status(ChatRoomStatus.ACTIVE)
                .type(ChatRoomType.INQUIRY)
                .createdAt(NOW)
                .deletedAt(null)
                .build();
    }

    @Nested
    @DisplayName("엔티티 → 도메인 변환")
    class ToDomainTest {

        @Test
        void 모든_필드를_올바르게_매핑한다() {
            ChatRoomJpaEntity entity = buildEntity(10L);

            ChatRoom result = mapper.toDomain(entity);

            assertThat(result.getId().value()).isEqualTo(10L);
            assertThat(result.getCreatorId().value()).isEqualTo(5L);
            assertThat(result.getProductId().value()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
            assertThat(result.getType()).isEqualTo(ChatRoomType.INQUIRY);
            assertThat(result.getCreatedAt()).isEqualTo(NOW);
            assertThat(result.getDeletedAt()).isNull();
        }

        @Test
        void 엔티티_ID가_0이면_INVALID_INPUT_예외() {
            assertThatThrownBy(() -> mapper.toDomain(buildEntity(0L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }
    }

    @Nested
    @DisplayName("도메인 → 엔티티 변환")
    class ToJpaEntityTest {

        @Test
        void 새_도메인의_모든_필드를_올바르게_매핑한다() {
            ChatRoom domain = ChatRoom.create(MemberId.of(5L), ProductId.of(1L), null, ChatRoomType.FUNDING, NOW);

            ChatRoomJpaEntity result = mapper.toJpaEntity(domain);

            assertThat(result.getId()).isNull();
            assertThat(result.getCreatorId()).isEqualTo(5L);
            assertThat(result.getProductId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
            assertThat(result.getType()).isEqualTo(ChatRoomType.FUNDING);
        }

        @Test
        void ID가_있는_도메인도_올바르게_매핑한다() {
            ChatRoomJpaEntity entity = buildEntity(10L);
            ChatRoom domain = mapper.toDomain(entity);

            ChatRoomJpaEntity result = mapper.toJpaEntity(domain);

            assertThat(result.getId()).isEqualTo(10L);
        }
    }
}
