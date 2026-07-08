package com.bds.auth.infrastructure.persistence.adapter;

import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.infrastructure.persistence.entity.AuthLocalJpaEntity;
import com.bds.auth.infrastructure.persistence.repository.AuthLocalJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLocalAdapter {

    private final AuthLocalJpaRepository authLocalJpaRepo;

    public AuthLocal save(AuthLocal authLocal) {

        AuthLocalJpaEntity jpaEntity = AuthLocalJpaEntity.from(authLocal);
        authLocalJpaRepo.save(jpaEntity);
        return authLocal;
    }
}