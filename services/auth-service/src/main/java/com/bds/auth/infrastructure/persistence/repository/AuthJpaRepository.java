package com.bds.auth.infrastructure.persistence.repository;

import com.bds.auth.infrastructure.persistence.entity.AuthJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthJpaRepository extends JpaRepository<AuthJpaEntity, Long> {

    boolean existsByEmail(String email);

}
