package com.bds.chat.infrastructure.persistence.chatroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ChatRoomJpaRepository extends JpaRepository<ChatRoomJpaEntity, Long> {

    Optional<ChatRoomJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}
