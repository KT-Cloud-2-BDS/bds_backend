package com.bds.auth.infrastructure.persistence.adapter;

import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.infrastructure.persistence.entity.AuthLocalJpaEntity;
import com.bds.auth.infrastructure.persistence.mapper.AuthLocalMapper;
import com.bds.auth.infrastructure.persistence.repository.AuthLocalJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLocalAdapter {

    private final AuthLocalJpaRepository authLocalJpaRepo;
    private final AuthLocalMapper authLocalMapper;

    public AuthLocal save(AuthLocal authLocal) {
        AuthLocalJpaEntity jpaEntity = AuthLocalMapper.toJpaEntity(authLocal);
        authLocalJpaRepo.save(jpaEntity);
        return authLocal;
    }

    public Optional<AuthLocal> findByAuthId(Long authId) {
        return authLocalJpaRepo.findByAuthId(authId)
            .map(AuthLocalMapper::toDomain);
    }
}