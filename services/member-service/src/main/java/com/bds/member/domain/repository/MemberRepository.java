package com.bds.member.domain.repository;


import com.bds.member.domain.entity.Member;
import java.util.Optional;

public interface MemberRepository{

    void save(Member member);

    boolean existsByNickname(String nickname);

    Optional<Member> findByAuthId(Long authId);

    boolean existsByAuthId(Long authId);

    void softDeleteByAuthId(Long authId);
}

