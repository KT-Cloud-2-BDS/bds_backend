package com.bds.chat.persistence.repository;

import com.bds.chat.persistence.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByIdAndDeletedAtIsNull(Long id);
}
