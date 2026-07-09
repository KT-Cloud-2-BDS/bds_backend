package com.bds.chat.infrastructure.persistence.chatroom;

import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.shared.ChatRoomId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatRoomPersistenceAdapter implements ChatRoomRepository {

    private final ChatRoomJpaRepository jpaRepository;
    private final ChatRoomMapper mapper;

    @Override
    public Optional<ChatRoom> findActiveById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ChatRoom> findActiveByIdForUpdate(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNullForUpdate(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ChatRoom> findInquiryRoomByProductAndBuyer(Long productId, Long buyerId) {
        return jpaRepository.findByProductIdAndCreatorIdAndTypeAndDeletedAtIsNull(productId, buyerId, ChatRoomType.INQUIRY)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ChatRoom> findFundingRoomByProduct(Long productId) {
        return jpaRepository.findByProductIdAndTypeAndDeletedAtIsNull(productId, ChatRoomType.FUNDING)
                .map(mapper::toDomain);
    }

    @Override
    public List<ChatRoom> findActiveByIds(List<Long> ids, Long cursor, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        List<ChatRoomJpaEntity> entities = cursor == null
                ? jpaRepository.findActiveByIds(ids, page)
                : jpaRepository.findActiveByIdsBeforeCursor(ids, cursor, page);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        ChatRoomJpaEntity saved = jpaRepository.save(mapper.toJpaEntity(chatRoom));
        if (chatRoom.getId() == null) {
            chatRoom.assignId(ChatRoomId.of(saved.getId()));
        }
        return mapper.toDomain(saved);
    }
}
