package com.bds.chat.infrastructure.persistence.blacklist;

import com.bds.chat.domain.blackList.BlacklistStatus;
import com.bds.chat.domain.blackList.FundingChatBlacklist;
import com.bds.chat.domain.blackList.FundingChatBlacklistRepository;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FundingChatBlacklistPersistenceAdapter implements FundingChatBlacklistRepository {

    private final FundingChatBlacklistJpaRepository jpaRepository;
    private final FundingChatBlacklistMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean isBlacklisted(Long roomId, Long memberId) {
        return jpaRepository.existsByRoom_IdAndMemberIdAndStatus(roomId, memberId, BlacklistStatus.ACTIVE);
    }

    @Override
    public Optional<FundingChatBlacklist> findBlacklist(Long roomId, Long memberId) {
        return jpaRepository.findByRoom_IdAndMemberId(roomId, memberId)
                .map(mapper::toDomain);
    }

    @Override
    public FundingChatBlacklist save(FundingChatBlacklist blacklist) {
        ChatRoomJpaEntity roomRef = entityManager.getReference(ChatRoomJpaEntity.class, blacklist.getRoomId());
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(blacklist, roomRef)));
    }
}
