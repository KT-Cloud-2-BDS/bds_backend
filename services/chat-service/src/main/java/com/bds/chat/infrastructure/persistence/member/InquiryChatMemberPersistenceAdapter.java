package com.bds.chat.infrastructure.persistence.member;

import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public List<InquiryChatMember> findActiveMembers(Long roomId) {
        return jpaRepository.findByRoom_IdAndDeletedAtIsNull(roomId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public InquiryChatMember save(InquiryChatMember member) {
        ChatRoomJpaEntity roomRef = entityManager.getReference(ChatRoomJpaEntity.class, member.getRoomId());
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(member, roomRef)));
    }
}
