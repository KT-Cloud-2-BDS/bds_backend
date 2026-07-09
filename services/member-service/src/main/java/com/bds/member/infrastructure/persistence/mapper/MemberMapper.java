package com.bds.member.infrastructure.persistence.mapper;

import com.bds.member.domain.entity.Member;
import com.bds.member.infrastructure.persistence.entity.MemberJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {
    public Member toDomain(MemberJpaEntity jpaEntity) {
        if (jpaEntity == null) return null;

        return Member.of(
            jpaEntity.getId(),
            jpaEntity.getAuthId(),
            jpaEntity.getNickname()
        );
    }

    public MemberJpaEntity toJpaEntity(Member domain) {
        if (domain == null) return null;

        return MemberJpaEntity.builder()
            .id(domain.getId())
            .authId(domain.getAuthId())
            .nickname(domain.getNickname())
            .build();
    }
}
