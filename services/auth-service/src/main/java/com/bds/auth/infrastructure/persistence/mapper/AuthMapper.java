package com.bds.auth.infrastructure.persistence.mapper;

import com.bds.auth.domain.entity.Auth;
import com.bds.auth.infrastructure.persistence.entity.AuthJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public static Auth toDomain(AuthJpaEntity jpaEntity) {
        if (jpaEntity == null) return null;

        return Auth.of(
            jpaEntity.getId(),
            jpaEntity.getEmail(),
            jpaEntity.getStatus(),
            jpaEntity.getRole()
        );
    }

    public static AuthJpaEntity toJpaEntity(Auth domain) {
        if (domain == null) return null;

        return AuthJpaEntity.builder()
            .id(domain.getId())
            .email(domain.getEmail())
            .status(domain.getStatus())
            .role(domain.getRole())
            .build();
    }
}
