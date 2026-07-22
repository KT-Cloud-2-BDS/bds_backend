package com.bds.auth.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 테스트에서만 쓰는 1회용 RSA 키를 매 실행마다 새로 생성한다.
 * 실제 서비스와 무관한 값이라도 PEM을 여러 테스트 파일에 복붙해 하드코딩하면
 * 중복 관리 및 시크릿 스캐너 오탐의 대상이 되므로, 이 유틸을 통해서만 발급받는다.
 */
public final class TestRsaKeyGenerator {

    private TestRsaKeyGenerator() {
    }

    public static String generatePrivateKeyPem() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            String base64Body = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            return "-----BEGIN PRIVATE KEY-----\n" + base64Body + "\n-----END PRIVATE KEY-----\n";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("테스트용 RSA 키 생성에 실패했습니다.", e);
        }
    }
}
