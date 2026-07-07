package com.bds.auth.infrastructure.persistence.mapper;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.infrastructure.persistence.entity.AuthLocalJpaEntity;

public class AuthLocalMapper {

    public static AuthLocal toDomain(AuthLocalJpaEntity jpaEntity) {
        if (jpaEntity == null) return null;
        Auth authDomain = AuthMapper.toDomain(jpaEntity.getAuthJpaEntity());

        return AuthLocal.of(
            jpaEntity.getId(),
            jpaEntity.getPassword(),
            authDomain
        );
    }

    public static AuthLocalJpaEntity toJpaEntity(AuthLocal domain) {
        if (domain == null) return null;

        return AuthLocalJpaEntity.builder()
            .id(domain.getId())
            .password(domain.getPassword())
            .authJpaEntity(AuthMapper.toJpaEntity(domain.getAuth()))
            .build();
    }

}
