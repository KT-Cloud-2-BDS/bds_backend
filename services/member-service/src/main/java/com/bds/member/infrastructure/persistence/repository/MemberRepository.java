package com.bds.member.infrastructure.persistence.repository;

import com.bds.member.infrastructure.persistence.entity.MemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberJpaEntity, Long> {

    boolean existsByNickname(String nickname);

}
