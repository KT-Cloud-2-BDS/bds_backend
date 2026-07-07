package com.bds.auth.infrastructure.persistence.adapter;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.infrastructure.persistence.entity.AuthJpaEntity;
import com.bds.auth.infrastructure.persistence.mapper.AuthMapper;
import com.bds.auth.infrastructure.persistence.repository.AuthJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthAdapter {

    private final AuthJpaRepository authJpaRepo;

    public boolean existsByEmail(String email) {
        return authJpaRepo.existsByEmail(email);
    }


}
