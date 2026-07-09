package com.bds.chat.infrastructure.persistence.member;

import com.bds.chat.application.message.dto.ReadReceiptDto;
import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.shared.InquiryChatMemberId;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InquiryChatMemberPersistenceAdapter implements InquiryChatMemberRepository {

    private final InquiryChatMemberJpaRepository jpaRepository;
    private final InquiryChatMemberMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<InquiryChatMember> findActiveMember(Long roomId, Long memberId) {
        return jpaRepository.findByRoom_IdAndMemberIdAndDeletedAtIsNull(roomId, memberId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveMember(Long roomId, Long memberId) {
        return jpaRepository.existsByRoom_IdAndMemberIdAndDeletedAtIsNull(roomId, memberId);
    }

    @Override
    public Optional<InquiryChatMember> findByRoomIdAndMemberId(Long roomId, Long memberId) {
        return jpaRepository.findByRoom_IdAndMemberId(roomId, memberId)
                .map(mapper::toDomain);
    }

    @Override
    public List<InquiryChatMember> findAllByRoomId(Long roomId) {
        return jpaRepository.findByRoom_Id(roomId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<InquiryChatMember> findActiveMembers(Long roomId) {
        return jpaRepository.findByRoom_IdAndDeletedAtIsNull(roomId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<InquiryChatMember> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberIdAndDeletedAtIsNull(memberId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<InquiryChatMember> findActiveMembersByRoomIds(List<Long> roomIds) {
        return jpaRepository.findByRoom_IdInAndDeletedAtIsNull(roomIds)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public InquiryChatMember save(InquiryChatMember member) {
        ChatRoomJpaEntity roomRef = entityManager.getReference(ChatRoomJpaEntity.class, member.getRoomId().value());
        InquiryChatMemberJpaEntity saved = jpaRepository.save(mapper.toJpaEntity(member, roomRef));
        if (member.getId() == null) {
            member.assignId(InquiryChatMemberId.of(saved.getId()));
            return member;
        }
        return mapper.toDomain(saved);
    }

    @Override
    public void bulkUpdateLastRead(List<ReadReceiptDto> batch) {
        Long[]          roomIds    = batch.stream().map(ReadReceiptDto::roomId).toArray(Long[]::new);
        Long[]          memberIds  = batch.stream().map(ReadReceiptDto::userId).toArray(Long[]::new);
        Long[]          messageIds = batch.stream().map(ReadReceiptDto::lastReadMessageId).toArray(Long[]::new);
        LocalDateTime[] readAts    = batch.stream()
                .map(r->LocalDateTime.ofInstant(r.readAt(), ZoneOffset.UTC))
                .toArray(LocalDateTime[]::new);
        jpaRepository.bulkUpdateLastRead(roomIds, memberIds, messageIds, readAts);
    }
}
