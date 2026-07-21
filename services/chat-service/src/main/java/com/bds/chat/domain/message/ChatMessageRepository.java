package com.bds.chat.domain.message;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChatMessageRepository {

    List<ChatMessage> findActiveMessagesByRoomId(Long roomId);

    Optional<ChatMessage> findById(Long id);

    Optional<ChatMessage> findByClientId(String clientId);

    List<ChatMessage> findByRoomIdBefore(Long roomId, Long cursor, int limit);

    List<ChatMessage> findBySenderIdBefore(Long senderId, Long cursor, int limit);

    LatestWithUnread findLatestWithUnread(Long roomId, Long lastReadId);

    Map<Long, LatestWithUnread> findLatestWithUnreadBatch(List<Long> roomIds, Map<Long, Long> lastReadByRoomId);

    boolean existsByClientId(String clientId);

    ChatMessage save(ChatMessage chatMessage);
}
