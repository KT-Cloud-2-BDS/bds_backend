package com.bds.auth.infrastructure.persistence.repository;

import com.bds.auth.infrastructure.persistence.entity.AuthSocialJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSocialJpaRepository extends JpaRepository<AuthSocialJpaEntity, Long> {

    Optional<AuthSocialJpaEntity> findByProviderAndProviderId(String provider, String providerId);
}
