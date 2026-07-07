package com.bds.auth.infrastructure.persistence.adapter;

import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.infrastructure.persistence.entity.AuthLocalJpaEntity;
import com.bds.auth.infrastructure.persistence.mapper.AuthLocalMapper;
import com.bds.auth.infrastructure.persistence.repository.AuthLocalJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLocalAdapter {

    private final AuthLocalJpaRepository authLocalJpaRepo;

    public AuthLocal save(AuthLocal authLocal) {
        AuthLocalJpaEntity jpaEntity = AuthLocalMapper.toJpaEntity(authLocal);
        AuthLocalJpaEntity savedEntity = authLocalJpaRepo.save(jpaEntity);
        return AuthLocalMapper.toDomain(savedEntity);
    }
}