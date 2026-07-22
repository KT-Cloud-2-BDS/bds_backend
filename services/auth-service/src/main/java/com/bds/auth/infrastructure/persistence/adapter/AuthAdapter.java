package com.bds.auth.infrastructure.persistence.adapter;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.enums.Status;
import com.bds.auth.domain.repository.AuthRepository;
import com.bds.auth.infrastructure.persistence.entity.AuthJpaEntity;
import com.bds.auth.infrastructure.persistence.mapper.AuthMapper;
import com.bds.auth.infrastructure.persistence.repository.AuthJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthAdapter implements AuthRepository {

    private final AuthJpaRepository authJpaRepo;
    private final AuthMapper authMapper;

    @Override
    public boolean existsByEmailAndStatus(String email, Status status) {
        return authJpaRepo.existsByEmailAndStatus(email, status);
    }

    @Override
    public Auth save(Auth auth) {
        AuthJpaEntity jpaEntity = authMapper.toJpaEntity(auth);
        AuthJpaEntity savedEntity = authJpaRepo.save(jpaEntity);
        return authMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Auth> findByEmail(String email){
        return authJpaRepo.findByEmail(email)
            .map(entity -> authMapper.toDomain(entity));
    }

    @Override
    public Optional<Auth> findById(Long authId) {
        return authJpaRepo.findById(authId)
            .map(entity -> authMapper.toDomain(entity));
    }

    @Override
    public void softDelete(Long authId) {
        authJpaRepo.softDeleteById(authId, Status.DELETED);
    }

}
