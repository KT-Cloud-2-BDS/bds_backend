package com.bds.chat.infrastructure.persistence.message;

import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.ChatMessageRepository;
import com.bds.chat.domain.message.LatestWithUnread;
import com.bds.chat.domain.message.MessageStatus;
import com.bds.chat.domain.message.MessageType;
import com.bds.chat.domain.shared.ChatMessageId;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatMessagePersistenceAdapter implements ChatMessageRepository {

    private final ChatMessageJpaRepository jpaRepository;
    private final ChatMessageMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ChatMessage> findActiveMessagesByRoomId(Long roomId) {
        return jpaRepository.findByRoom_IdAndDeletedAtIsNullOrderByCreatedAtAsc(roomId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public LatestWithUnread findLatestWithUnread(Long roomId, Long lastReadId) {
        long effectiveLastReadId = lastReadId != null ? lastReadId : 0L;
        return jpaRepository.findLatestWithUnread(roomId, effectiveLastReadId)
                .map(p -> new LatestWithUnread(projectToDomain(p), p.getUnreadCount()))
                .orElse(new LatestWithUnread(null, 0L));
    }

    @Override
    public Map<Long, LatestWithUnread> findLatestWithUnreadBatch(List<Long> roomIds, Map<Long, Long> lastReadByRoomId) {
        Long[] roomIdArr = roomIds.toArray(Long[]::new);
        Long[] lastReadArr = roomIds.stream()
                .map(id -> lastReadByRoomId.getOrDefault(id, 0L))
                .toArray(Long[]::new);

        return jpaRepository.findLatestWithUnreadBatch(roomIdArr, lastReadArr)
                .stream()
                .collect(Collectors.toMap(
                        MessageWithUnreadProjection::getRoomId,
                        p -> new LatestWithUnread(projectToDomain(p), p.getUnreadCount())
                ));
    }

    @Override
    public boolean existsByClientId(String clientId) {
        return jpaRepository.existsByClientId(clientId);
    }

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        ChatRoomJpaEntity roomRef = entityManager.getReference(ChatRoomJpaEntity.class, chatMessage.getRoomId().value());
        ChatMessageJpaEntity saved = jpaRepository.save(mapper.toJpaEntity(chatMessage, roomRef));
        if (chatMessage.getId() == null) {
            chatMessage.assignId(ChatMessageId.of(saved.getId()));
            return chatMessage;
        }
        return mapper.toDomain(saved);
    }

    private ChatMessage projectToDomain(MessageWithUnreadProjection p) {
        return ChatMessage.restore(
                ChatMessageId.of(p.getMessageId()),
                ChatRoomId.of(p.getRoomId()),
                p.getSenderId() != null ? MemberId.of(p.getSenderId()) : null,
                p.getContent(),
                MessageType.valueOf(p.getType()),
                p.getClientId(),
                MessageStatus.valueOf(p.getStatus()),
                p.getCreatedAt(),
                null
        );
    }
}
