package com.bds.chat.infrastructure.persistence.chatroom;

import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomType;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatRoomPersistenceAdapterUnitExceptionTest {

    @Mock ChatRoomJpaRepository jpaRepository;
    @Mock ChatRoomMapper mapper;

    @InjectMocks ChatRoomPersistenceAdapter adapter;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("findActiveById 실패 케이스")
    class FindActiveByIdFailTest {

        @Test
        void 삭제된_방은_빈_Optional을_반환한다() {
            given(jpaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

            Optional<ChatRoom> result = adapter.findActiveById(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findFundingRoomByProduct 실패 케이스")
    class FindFundingRoomByProductFailTest {

        @Test
        void 펀딩_채팅방이_없으면_빈_Optional을_반환한다() {
            given(jpaRepository.findByProductIdAndType(99L, ChatRoomType.FUNDING)).willReturn(Optional.empty());

            Optional<ChatRoom> result = adapter.findFundingRoomByProduct(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveByIds 실패 케이스")
    class FindActiveByIdsFailTest {

        @Test
        void 일치하는_방이_없으면_빈_리스트를_반환한다() {
            given(jpaRepository.findActiveByIds(eq(List.of(99L)), any())).willReturn(List.of());

            List<ChatRoom> result = adapter.findActiveByIds(List.of(99L), null, 10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findInquiryRoomByProductAndBuyer 실패 케이스")
    class FindInquiryRoomByProductAndBuyerFailTest {

        @Test
        void 문의_채팅방이_없으면_빈_Optional을_반환한다() {
            given(jpaRepository.findByProductIdAndCreatorIdAndType(1L, 99L, ChatRoomType.INQUIRY))
                    .willReturn(Optional.empty());

            Optional<ChatRoom> result = adapter.findInquiryRoomByProductAndBuyer(1L, 99L);

            assertThat(result).isEmpty();
        }
    }
}
