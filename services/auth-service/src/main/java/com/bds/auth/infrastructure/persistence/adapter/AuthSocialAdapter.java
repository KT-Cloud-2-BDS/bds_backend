package com.bds.auth.infrastructure.persistence.adapter;

import com.bds.auth.domain.entity.AuthSocial;
import com.bds.auth.domain.repository.AuthSocialRepository;
import com.bds.auth.infrastructure.persistence.entity.AuthSocialJpaEntity;
import com.bds.auth.infrastructure.persistence.mapper.AuthSocialMapper;
import com.bds.auth.infrastructure.persistence.repository.AuthSocialJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthSocialAdapter implements AuthSocialRepository {

    private final AuthSocialJpaRepository authSocialJpaRepo;

    @Override
    public AuthSocial save(AuthSocial authSocial) {
        AuthSocialJpaEntity jpaEntity = AuthSocialMapper.toJpaEntity(authSocial);
        AuthSocialJpaEntity savedEntity = authSocialJpaRepo.save(jpaEntity);
        return AuthSocialMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AuthSocial> findByProviderAndProviderId(String provider, String providerId) {
        return authSocialJpaRepo.findByProviderAndProviderId(provider, providerId)
            .map(AuthSocialMapper::toDomain);
    }
}
