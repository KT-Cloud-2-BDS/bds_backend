package com.bds.auth.infrastructure.persistence.repository;

import com.bds.auth.infrastructure.persistence.entity.AuthLocalJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthLocalJpaRepository extends JpaRepository<AuthLocalJpaEntity, Long> {

    Optional<AuthLocalJpaEntity> findByAuthId(Long authId);

    @Modifying
    @Query("DELETE FROM AuthLocalJpaEntity a WHERE a.authId = :authId")
    void deleteByAuthId(@Param("authId") Long authId);
}
