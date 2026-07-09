package com.bds.auth.infrastructure.persistence.repository;

import com.bds.auth.infrastructure.persistence.entity.AuthJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthJpaRepository extends JpaRepository<AuthJpaEntity, Long> {

    boolean existsByEmail(String email);

    Optional<AuthJpaEntity> findByEmail(String email);

    Optional<AuthJpaEntity> findById(Long authId);
}
