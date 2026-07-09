package com.bds.chat.infrastructure.persistence.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, Long> {

    List<ChatMessageJpaEntity> findByRoom_IdAndDeletedAtIsNullOrderByCreatedAtAsc(Long roomId);

    boolean existsByClientId(String clientId);
}
