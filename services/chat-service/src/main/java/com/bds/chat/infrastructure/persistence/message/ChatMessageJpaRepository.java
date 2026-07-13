package com.bds.chat.infrastructure.persistence.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, Long> {

    List<ChatMessageJpaEntity> findByRoom_IdAndDeletedAtIsNullOrderByCreatedAtAsc(Long roomId);

    Optional<ChatMessageJpaEntity> findByClientId(String clientId);

    List<ChatMessageJpaEntity> findByRoom_IdAndDeletedAtIsNullOrderByIdDesc(Long roomId, Pageable pageable);

    List<ChatMessageJpaEntity> findByRoom_IdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long roomId, Long cursor, Pageable pageable);

    List<ChatMessageJpaEntity> findBySenderIdAndDeletedAtIsNullOrderByIdDesc(Long senderId, Pageable pageable);

    List<ChatMessageJpaEntity> findBySenderIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long senderId, Long cursor, Pageable pageable);

    @Query(value = """
            SELECT
                m.id          AS messageId,
                m.room_id     AS roomId,
                m.sender_id   AS senderId,
                m.content,
                m.type,
                m.client_id   AS clientId,
                m.status,
                m.created_at  AS createdAt,
                COALESCE(u.unread_count, 0) AS unreadCount
            FROM chat_message m
            LEFT JOIN (
                SELECT room_id, COUNT(*) AS unread_count
                FROM chat_message
                WHERE room_id = :roomId AND deleted_at IS NULL AND id > COALESCE(:lastReadId, 0)
                GROUP BY room_id
            ) u ON m.room_id = u.room_id
            WHERE m.room_id = :roomId AND m.deleted_at IS NULL
            ORDER BY m.id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<MessageWithUnreadProjection> findLatestWithUnread(@Param("roomId") Long roomId,
                                                               @Param("lastReadId") Long lastReadId);

    @Query(value = """
            WITH room_thresholds AS (
                SELECT * FROM unnest(CAST(:roomIds AS bigint[]), CAST(:lastReadIds AS bigint[])) AS t(room_id, last_read_id)
            ),
            latest_messages AS (
                SELECT DISTINCT ON (room_id)
                    id, room_id, sender_id, content, type, client_id, status, created_at
                FROM chat_message
                WHERE room_id = ANY(:roomIds) AND deleted_at IS NULL
                ORDER BY room_id, id DESC
            ),
            unread_counts AS (
                SELECT cm.room_id, COUNT(*) AS unread_count
                FROM chat_message cm
                JOIN room_thresholds rt ON cm.room_id = rt.room_id
                WHERE cm.deleted_at IS NULL AND cm.id > COALESCE(rt.last_read_id, 0)
                GROUP BY cm.room_id
            )
            SELECT
                lm.id         AS messageId,
                lm.room_id    AS roomId,
                lm.sender_id  AS senderId,
                lm.content,
                lm.type,
                lm.client_id  AS clientId,
                lm.status,
                lm.created_at AS createdAt,
                COALESCE(uc.unread_count, 0) AS unreadCount
            FROM latest_messages lm
            LEFT JOIN unread_counts uc ON lm.room_id = uc.room_id
            """, nativeQuery = true)
    List<MessageWithUnreadProjection> findLatestWithUnreadBatch(@Param("roomIds") Long[] roomIds,
                                                                @Param("lastReadIds") Long[] lastReadIds);

    boolean existsByClientId(String clientId);
}
