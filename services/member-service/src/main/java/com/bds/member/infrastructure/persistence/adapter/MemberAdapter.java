package com.bds.member.infrastructure.persistence.adapter;

import com.bds.member.domain.entity.Member;
import com.bds.member.infrastructure.persistence.entity.MemberJpaEntity;
import com.bds.member.infrastructure.persistence.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAdapter {

    private final MemberRepository memberRepo;

    public void save(Member member) {
        MemberJpaEntity jpaEntity = MemberJpaEntity.from(member);
        memberRepo.save(jpaEntity);
    }

    public boolean existsByNickname(String nickname) {
        return memberRepo.existsByNickname(nickname);
    }
}
