package com.bds.auth.infrastructure.persistence.repository;

import com.bds.auth.domain.entity.enums.Status;
import com.bds.auth.infrastructure.persistence.entity.AuthJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthJpaRepository extends JpaRepository<AuthJpaEntity, Long> {

    boolean existsByEmailAndStatus(String email, Status status);

    Optional<AuthJpaEntity> findByEmail(String email);

    Optional<AuthJpaEntity> findById(Long authId);
}

