package com.bds.chat.infrastructure.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface InquiryChatMemberJpaRepository extends JpaRepository<InquiryChatMemberJpaEntity, Long> {

    Optional<InquiryChatMemberJpaEntity> findByRoom_IdAndMemberIdAndDeletedAtIsNull(Long roomId, Long memberId);

    Optional<InquiryChatMemberJpaEntity> findByRoom_IdAndMemberId(Long roomId, Long memberId);

    List<InquiryChatMemberJpaEntity> findByRoom_Id(Long roomId);

    List<InquiryChatMemberJpaEntity> findByRoom_IdAndDeletedAtIsNull(Long roomId);

    List<InquiryChatMemberJpaEntity> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<InquiryChatMemberJpaEntity> findByRoom_IdInAndDeletedAtIsNull(List<Long> roomIds);

    boolean existsByRoom_IdAndMemberIdAndDeletedAtIsNull(Long roomId, Long memberId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE inquiry_chat_member AS m
            SET last_read_message_id = v.msg_id,
                updated_at            = v.read_at
            FROM unnest(:roomIds::bigint[], :memberIds::bigint[], :messageIds::bigint[], :readAts::timestamp[])
                 AS v(room_id, member_id, msg_id, read_at)
            WHERE m.room_id    = v.room_id
              AND m.member_id  = v.member_id
              AND m.deleted_at IS NULL
              AND (m.last_read_message_id IS NULL OR m.last_read_message_id < v.msg_id)
            """, nativeQuery = true)
    void bulkUpdateLastRead(@Param("roomIds")    Long[] roomIds,
                            @Param("memberIds")  Long[] memberIds,
                            @Param("messageIds") Long[] messageIds,
                            @Param("readAts")    LocalDateTime[] readAts);
}
