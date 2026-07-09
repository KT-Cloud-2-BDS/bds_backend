package com.bds.chat.infrastructure.persistence.chatroom;

import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public ChatRoom save(ChatRoom chatRoom) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(chatRoom)));
    }
}
