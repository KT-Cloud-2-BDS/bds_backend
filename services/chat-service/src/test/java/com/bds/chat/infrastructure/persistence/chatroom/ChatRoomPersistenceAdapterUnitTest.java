package com.bds.chat.infrastructure.persistence.chatroom;

import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatRoomPersistenceAdapterUnitTest {

    @Mock ChatRoomJpaRepository jpaRepository;
    @Mock ChatRoomMapper mapper;

    @InjectMocks ChatRoomPersistenceAdapter adapter;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    private ChatRoomJpaEntity roomEntity(Long id) {
        return ChatRoomJpaEntity.builder()
                .id(id).creatorId(2L).productId(1L)
                .status(ChatRoomStatus.ACTIVE).type(ChatRoomType.INQUIRY).createdAt(NOW)
                .build();
    }

    private ChatRoom roomDomain(Long id) {
        ChatRoom domain = ChatRoom.create(MemberId.of(2L), ProductId.of(1L), null, ChatRoomType.INQUIRY, NOW);
        if (id != null) domain.assignId(ChatRoomId.of(id));
        return domain;
    }

    @Nested
    @DisplayName("findActiveById")
    class FindActiveByIdTest {

        @Test
        void 활성_채팅방을_도메인으로_반환한다() {
            ChatRoomJpaEntity entity = roomEntity(10L);
            ChatRoom domain = roomDomain(10L);
            given(jpaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            Optional<ChatRoom> result = adapter.findActiveById(10L);

            assertThat(result).isPresent();
            assertThat(result.get().getId().value()).isEqualTo(10L);
        }

        @Test
        void 존재하지_않으면_빈_Optional을_반환한다() {
            given(jpaRepository.findByIdAndDeletedAtIsNull(99L)).willReturn(Optional.empty());

            Optional<ChatRoom> result = adapter.findActiveById(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findFundingRoomByProduct")
    class FindFundingRoomByProductTest {

        @Test
        void 펀딩_채팅방을_도메인으로_반환한다() {
            ChatRoomJpaEntity entity = roomEntity(20L);
            ChatRoom domain = roomDomain(20L);
            given(jpaRepository.findByProductIdAndType(1L, ChatRoomType.FUNDING)).willReturn(Optional.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            Optional<ChatRoom> result = adapter.findFundingRoomByProduct(1L);

            assertThat(result).isPresent();
        }

        @Test
        void 펀딩방이_없으면_빈_Optional을_반환한다() {
            given(jpaRepository.findByProductIdAndType(1L, ChatRoomType.FUNDING)).willReturn(Optional.empty());

            Optional<ChatRoom> result = adapter.findFundingRoomByProduct(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveByIds")
    class FindActiveByIdsTest {

        @Test
        void 커서_없이_채팅방_목록을_반환한다() {
            List<Long> ids = List.of(1L, 2L);
            ChatRoomJpaEntity entity = roomEntity(1L);
            ChatRoom domain = roomDomain(1L);
            given(jpaRepository.findActiveByIds(eq(ids), any())).willReturn(List.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            List<ChatRoom> result = adapter.findActiveByIds(ids, null, 10);

            assertThat(result).hasSize(1);
        }

        @Test
        void 커서와_함께_채팅방_목록을_반환한다() {
            List<Long> ids = List.of(1L, 2L);
            ChatRoomJpaEntity entity = roomEntity(1L);
            ChatRoom domain = roomDomain(1L);
            given(jpaRepository.findActiveByIdsBeforeCursor(eq(ids), eq(5L), any())).willReturn(List.of(entity));
            given(mapper.toDomain(entity)).willReturn(domain);

            List<ChatRoom> result = adapter.findActiveByIds(ids, 5L, 10);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTest {

        @Test
        void 새_채팅방_저장_시_ID를_할당하고_동일_객체를_반환한다() {
            ChatRoom newRoom = ChatRoom.create(MemberId.of(2L), ProductId.of(1L), null, ChatRoomType.INQUIRY, NOW);
            ChatRoomJpaEntity entityToSave = roomEntity(null);
            ChatRoomJpaEntity savedEntity = roomEntity(10L);

            given(mapper.toJpaEntity(newRoom)).willReturn(entityToSave);
            given(jpaRepository.save(entityToSave)).willReturn(savedEntity);

            ChatRoom result = adapter.save(newRoom);

            assertThat(result).isSameAs(newRoom);
            assertThat(result.getId().value()).isEqualTo(10L);
        }

        @Test
        void 기존_채팅방_저장_시_매핑된_도메인을_반환한다() {
            ChatRoom existing = roomDomain(10L);
            ChatRoomJpaEntity entityToSave = roomEntity(10L);
            ChatRoomJpaEntity savedEntity = roomEntity(10L);
            ChatRoom mappedBack = roomDomain(10L);

            given(mapper.toJpaEntity(existing)).willReturn(entityToSave);
            given(jpaRepository.save(entityToSave)).willReturn(savedEntity);
            given(mapper.toDomain(savedEntity)).willReturn(mappedBack);

            ChatRoom result = adapter.save(existing);

            assertThat(result).isSameAs(mappedBack);
        }
    }
}
