package com.bds.auth.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 서명/검증에 쓰이는 RSA 키를 설정값(PEM)에서 고정으로 로드해 단일 소스로 관리한다.
 * 키를 애플리케이션이 자체 생성하면 인스턴스마다, 재시작마다 키가 달라져서
 * 여러 대로 스케일 아웃하거나 재배포할 때 이미 발급된 토큰 검증이 깨진다.
 * JwtTokenUtil(서명)과 JwksService(공개키 노출)는 반드시 이 컴포넌트를 통해서만 키에 접근해야 한다.
 */
@Component
public class JwtKeyProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final RSAKey publicRsaKey;

    public JwtKeyProvider(@Value("${jwt.rsa.private-key}") String privateKeyPem) {
        try {
            this.privateKey = parsePrivateKey(privateKeyPem);
            this.publicKey = derivePublicKey((RSAPrivateCrtKey) this.privateKey);

            this.publicRsaKey = new RSAKey.Builder((RSAPublicKey) publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build();
        } catch (GeneralSecurityException | JOSEException e) {
            throw new IllegalStateException("RSA 키를 초기화할 수 없습니다.", e);
        }
    }

    private static PrivateKey parsePrivateKey(String pem) throws GeneralSecurityException {
        String base64Body = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(base64Body);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private static PublicKey derivePublicKey(RSAPrivateCrtKey privateCrtKey) throws GeneralSecurityException {
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
            privateCrtKey.getModulus(),
            privateCrtKey.getPublicExponent()
        );
        return KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
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
