package com.bds.member.infrastructure.persistence.repository;

import com.bds.member.infrastructure.persistence.entity.MemberJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<MemberJpaEntity, Long> {

    boolean existsByNickname(String nickname);

    Optional<MemberJpaEntity> findByAuthId(Long authId);

    boolean existsByAuthId(Long authId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberJpaEntity m SET m.isDeleted = true WHERE m.authId = :authId")
    void softDeleteByAuthId(@Param("authId") Long authId);
}
