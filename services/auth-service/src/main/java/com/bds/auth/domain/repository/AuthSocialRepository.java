package com.bds.auth.domain.repository;

import com.bds.auth.domain.entity.AuthSocial;
import java.util.Optional;

public interface AuthSocialRepository {
    AuthSocial save(AuthSocial authSocial);
    Optional<AuthSocial> findByProviderAndProviderId(String provider, String providerId);
}
