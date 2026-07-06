package com.bds.backend.auth.domain.repository;

import com.bds.backend.auth.domain.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<Auth, Long> {

    boolean existByEmail(String email);
}
