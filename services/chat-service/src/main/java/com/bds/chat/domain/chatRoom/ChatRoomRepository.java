package com.bds.chat.domain.chatRoom;

import java.util.Optional;

public interface ChatRoomRepository {

    Optional<ChatRoom> findActiveById(Long id);

    ChatRoom save(ChatRoom chatRoom);
}
