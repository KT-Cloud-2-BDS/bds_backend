package com.bds.chat.domain.message;

import java.util.List;
import java.util.Map;

public interface ChatMessageRepository {

    List<ChatMessage> findActiveMessagesByRoomId(Long roomId);

    LatestWithUnread findLatestWithUnread(Long roomId, Long lastReadId);

    Map<Long, LatestWithUnread> findLatestWithUnreadBatch(List<Long> roomIds, Map<Long, Long> lastReadByRoomId);

    boolean existsByClientId(String clientId);

    ChatMessage save(ChatMessage chatMessage);
}
