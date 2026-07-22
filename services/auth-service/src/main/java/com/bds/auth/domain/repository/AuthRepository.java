package com.bds.auth.domain.repository;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.enums.Status;
import java.util.Optional;

public interface AuthRepository {
    boolean existsByEmailAndStatus(String email, Status status);
    Optional<Auth> findByEmail(String email);
    Optional<Auth> findById(Long authId);
    Auth save(Auth auth);
    void softDelete(Long authId);
}