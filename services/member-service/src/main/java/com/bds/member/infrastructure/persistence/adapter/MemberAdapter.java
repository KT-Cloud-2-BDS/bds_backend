package com.bds.member.infrastructure.persistence.adapter;

import com.bds.member.domain.entity.Member;
import com.bds.member.infrastructure.persistence.entity.MemberJpaEntity;
import com.bds.member.infrastructure.persistence.mapper.MemberMapper;
import com.bds.member.infrastructure.persistence.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAdapter {

    private final MemberRepository memberRepo;
    private final MemberMapper memberMapper;

    public void save(Member member) {
        MemberJpaEntity jpaEntity =memberMapper.toJpaEntity(member);
        memberRepo.saveAndFlush(jpaEntity);
    }

    public boolean existsByNickname(String nickname) {
        return memberRepo.existsByNickname(nickname);
    }

    public Optional<Member> findByAuthId(Long authId) {
        return memberRepo.findByAuthId(authId)
            .map(memberMapper::toDomain);
    }

    public boolean existsByAuthId(Long authId) {
        return memberRepo.existsByAuthId(authId);
    }

    public void softDeleteByAuthId(Long authId) {
        memberRepo.softDeleteByAuthId(authId);
    }
}
