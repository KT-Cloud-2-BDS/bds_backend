package com.bds.auth.domain.repository;

import com.bds.auth.domain.entity.AuthLocal;
import java.util.Optional;

public interface AuthLocalRepository {
    AuthLocal save(AuthLocal authLocal);
    Optional<AuthLocal> findByAuthId(Long authId);
    void deleteByAuthId(Long authId);
}