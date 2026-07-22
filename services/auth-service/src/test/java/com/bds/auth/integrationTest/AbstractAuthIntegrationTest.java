package com.bds.auth.integrationTest;

import com.bds.auth.support.TestRsaKeyGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * jwt.rsa.private-key를 test/resources에 고정 PEM으로 박아두지 않고,
 * 컨텍스트 로딩 시점마다 새로 생성해 주입한다. 모든 auth-service 통합 테스트는 이 클래스를 상속한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractAuthIntegrationTest {

    @DynamicPropertySource
    static void registerTestRsaKey(DynamicPropertyRegistry registry) {
        registry.add("jwt.rsa.private-key", TestRsaKeyGenerator::generatePrivateKeyPem);
    }
}
