package com.bds.support;

import com.bds.config.TestRabbitTopologyConfig;
import com.bds.order.OrderApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(classes = OrderApplication.class)
@ActiveProfiles("test")
@Import(TestRabbitTopologyConfig.class)
public abstract class AbstractRabbitMQIntegrationTest {

    static final PostgreSQLContainer<?> postgres;
    static final RabbitMQContainer rabbitmq;

    static {
        postgres = new PostgreSQLContainer<>("postgres:18");
        postgres.start();

        rabbitmq = new RabbitMQContainer("rabbitmq:4.1-management");
        rabbitmq.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.virtual-host", () -> "/");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);

    }
}
