package com.bds.auth.infrastructure.persistence.repository;

import com.bds.auth.infrastructure.persistence.entity.AuthLocalJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLocalJpaRepository extends JpaRepository<AuthLocalJpaEntity, Long> {

    Optional<AuthLocalJpaEntity> findByAuthId(Long authId);
}
