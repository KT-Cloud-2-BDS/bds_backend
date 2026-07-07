package com.bds.chat.infrastructure.persistence.chatroom;

import com.bds.chat.domain.chatRoom.ChatRoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ChatRoomJpaRepository extends JpaRepository<ChatRoomJpaEntity, Long> {

    Optional<ChatRoomJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cr FROM ChatRoomJpaEntity cr WHERE cr.id = :id AND cr.deletedAt IS NULL")
    Optional<ChatRoomJpaEntity> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);

    Optional<ChatRoomJpaEntity> findByProductIdAndCreatorIdAndType(Long productId, Long creatorId, ChatRoomType type);

    Optional<ChatRoomJpaEntity> findByProductIdAndType(Long productId, ChatRoomType type);

    @Query("SELECT cr FROM ChatRoomJpaEntity cr WHERE cr.id IN :ids AND cr.deletedAt IS NULL ORDER BY cr.id DESC")
    List<ChatRoomJpaEntity> findActiveByIds(@Param("ids") List<Long> ids, Pageable pageable);

    @Query("SELECT cr FROM ChatRoomJpaEntity cr WHERE cr.id IN :ids AND cr.deletedAt IS NULL AND cr.id < :cursor ORDER BY cr.id DESC")
    List<ChatRoomJpaEntity> findActiveByIdsBeforeCursor(@Param("ids") List<Long> ids, @Param("cursor") Long cursor, Pageable pageable);
}
