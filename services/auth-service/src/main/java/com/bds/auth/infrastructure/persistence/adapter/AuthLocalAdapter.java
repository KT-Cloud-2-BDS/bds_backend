package com.bds.auth.infrastructure.persistence.adapter;

import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.domain.repository.AuthLocalRepository;
import com.bds.auth.infrastructure.persistence.entity.AuthLocalJpaEntity;
import com.bds.auth.infrastructure.persistence.mapper.AuthLocalMapper;
import com.bds.auth.infrastructure.persistence.repository.AuthLocalJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLocalAdapter implements AuthLocalRepository {

    private final AuthLocalJpaRepository authLocalJpaRepo;

    @Override
    public AuthLocal save(AuthLocal authLocal) {
        AuthLocalJpaEntity jpaEntity = AuthLocalMapper.toJpaEntity(authLocal);
        authLocalJpaRepo.save(jpaEntity);
        return authLocal;
    }

    @Override
    public Optional<AuthLocal> findByAuthId(Long authId) {
        return authLocalJpaRepo.findByAuthId(authId)
            .map(AuthLocalMapper::toDomain);
    }

    @Override
    public void deleteByAuthId(Long authId) {
        authLocalJpaRepo.deleteByAuthId(authId);
    }
}