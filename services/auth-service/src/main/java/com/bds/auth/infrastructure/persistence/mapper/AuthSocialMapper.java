package com.bds.auth.infrastructure.persistence.mapper;

import com.bds.auth.domain.entity.AuthSocial;
import com.bds.auth.infrastructure.persistence.entity.AuthSocialJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthSocialMapper {

    public static AuthSocial toDomain(AuthSocialJpaEntity jpaEntity) {
        if (jpaEntity == null) return null;
        return AuthSocial.of(
            jpaEntity.getId(),
            jpaEntity.getProviderId(),
            jpaEntity.getProvider(),
            jpaEntity.getEmail(),
            jpaEntity.getAuthId()
        );
    }

    public static AuthSocialJpaEntity toJpaEntity(AuthSocial domain) {
        if (domain == null) return null;

        return AuthSocialJpaEntity.builder()
            .id(domain.getId())
            .providerId(domain.getProviderId())
            .provider(domain.getProvider())
            .email(domain.getEmail())
            .authId(domain.getAuthId())
            .build();
    }

}
