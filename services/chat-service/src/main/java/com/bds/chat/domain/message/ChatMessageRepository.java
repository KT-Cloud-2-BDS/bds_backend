package com.bds.chat.domain.message;

import java.util.List;

public interface ChatMessageRepository {

    List<ChatMessage> findActiveMessagesByRoomId(Long roomId);

    boolean existsByClientId(String clientId);

    ChatMessage save(ChatMessage chatMessage);
}
