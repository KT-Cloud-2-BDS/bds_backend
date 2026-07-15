package com.bds.auth.domain.entity;

import lombok.Getter;

@Getter
public class AuthSocial {

    private Long id;
    private String providerId;
    private String provider;
    private String email;
    private Long authId;

    public static AuthSocial create(String providerId, String provider, String email, Long authId) {
        AuthSocial authSocial = new AuthSocial();
        authSocial.providerId = providerId;
        authSocial.provider = provider;
        authSocial.email = email;
        authSocial.authId = authId;
        return authSocial;
    }

    public static AuthSocial of(Long id, String providerId, String provider, String email, Long authId) {
        AuthSocial authSocial = new AuthSocial();
        authSocial.id = id;
        authSocial.providerId = providerId;
        authSocial.provider = provider;
        authSocial.email = email;
        authSocial.authId = authId;
        return authSocial;
    }
}
