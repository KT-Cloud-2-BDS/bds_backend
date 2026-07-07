package com.bds.auth.infrastructure.persistence.repository;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.infrastructure.persistence.entity.AuthLocalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLocalJpaRepository extends JpaRepository<AuthLocalJpaEntity, Long> {

}
