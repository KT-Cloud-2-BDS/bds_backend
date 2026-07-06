package com.bds.chat.infrastructure.persistence.message;

import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.ChatMessageRepository;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public boolean existsByClientId(String clientId) {
        return jpaRepository.existsByClientId(clientId);
    }

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        // FK 설정을 위해 프록시 참조 사용 (실제 SELECT 없이 참조만 획득)
        ChatRoomJpaEntity roomRef = entityManager.getReference(ChatRoomJpaEntity.class, chatMessage.getRoomId());
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(chatMessage, roomRef)));
    }
}
