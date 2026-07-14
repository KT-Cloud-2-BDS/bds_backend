package com.bds.auth.infrastructure.persistence.mapper;

import com.bds.auth.domain.entity.AuthLocal;
import com.bds.auth.infrastructure.persistence.entity.AuthLocalJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthLocalMapper {

    public static AuthLocal toDomain(AuthLocalJpaEntity jpaEntity) {
        if (jpaEntity == null) return null;
        return AuthLocal.of(
            jpaEntity.getId(),
            jpaEntity.getPassword(),
            jpaEntity.getAuthId()
        );
    }

    public static AuthLocalJpaEntity toJpaEntity(AuthLocal domain) {
        if (domain == null) return null;

        return AuthLocalJpaEntity.builder()
            .id(domain.getId())
            .password(domain.getPassword())
            .authId(domain.getAuthId())
            .build();
    }

}
