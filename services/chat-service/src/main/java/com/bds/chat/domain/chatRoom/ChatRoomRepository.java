package com.bds.chat.domain.chatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository {

    Optional<ChatRoom> findActiveById(Long id);

    Optional<ChatRoom> findActiveByIdForUpdate(Long id);

    Optional<ChatRoom> findInquiryRoomByProductAndBuyer(Long productId, Long buyerId);

    Optional<ChatRoom> findFundingRoomByProduct(Long productId);

    List<ChatRoom> findActiveByIds(List<Long> ids, Long cursor, int limit);

    ChatRoom save(ChatRoom chatRoom);
}
