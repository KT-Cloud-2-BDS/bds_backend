package com.bds.auth.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.stereotype.Component;

/**
 * JWT 서명/검증에 쓰이는 RSA 키를 애플리케이션 인스턴스 안에서 단일 소스로 관리한다.
 * JwtTokenUtil(서명)과 JwksService(공개키 노출)가 서로 다른 키를 생성하면 kid가 어긋나므로
 * 두 곳 모두 이 컴포넌트를 통해서만 키에 접근해야 한다.
 */
@Component
public class JwtKeyProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final RSAKey publicRsaKey;

    public JwtKeyProvider() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();

            this.publicRsaKey = new RSAKey.Builder((RSAPublicKey) publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build();
        } catch (NoSuchAlgorithmException | JOSEException e) {
            throw new IllegalStateException("RSA 키를 초기화할 수 없습니다.", e);
        }
    }

    public String getKeyId() {
        return publicRsaKey.getKeyID();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public JWKSet getPublicJWKSet() {
        return new JWKSet(publicRsaKey);
    }
}
