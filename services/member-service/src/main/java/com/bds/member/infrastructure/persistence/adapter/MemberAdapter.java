package com.bds.member.infrastructure.persistence.adapter;

import com.bds.member.domain.entity.Member;
import com.bds.member.domain.repository.MemberRepository;
import com.bds.member.infrastructure.persistence.entity.MemberJpaEntity;
import com.bds.member.infrastructure.persistence.mapper.MemberMapper;
import com.bds.member.infrastructure.persistence.repository.MemberJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAdapter implements MemberRepository {

    private final MemberJpaRepository memberRepo;
    private final MemberMapper memberMapper;

    @Override
    public void save(Member member) {
        MemberJpaEntity jpaEntity =memberMapper.toJpaEntity(member);
        memberRepo.saveAndFlush(jpaEntity);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return memberRepo.existsByNickname(nickname);
    }


    @Override
    public Optional<Member> findByAuthId(Long authId) {
        return memberRepo.findByAuthId(authId)
            .map(memberMapper::toDomain);
    }
    @Override
    public boolean existsByAuthId(Long authId) {
        return memberRepo.existsByAuthId(authId);
    }

    @Override
    public void softDeleteByAuthId(Long authId) {
        memberRepo.softDeleteByAuthId(authId);
    }
}
