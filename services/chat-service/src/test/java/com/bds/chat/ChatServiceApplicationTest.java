package com.bds.chat;

import com.bds.chat.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
class ChatServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}
