package com.bds.chat.persistence.repository;

import com.bds.chat.persistence.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long roomId);

    boolean existsByClientId(String clientId);
}
