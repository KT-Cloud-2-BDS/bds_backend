package com.bds.payment.support;

import com.bds.payment.PaymentServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(classes = PaymentServiceApplication.class)
@ActiveProfiles("test")
public abstract class AbstractPaymentRabbitMQIntegrationTest {

    static final MySQLContainer<?> mysql;
    static final RabbitMQContainer rabbitmq;

    static {
        mysql = new MySQLContainer<>("mysql:9.7");
        mysql.start();

        rabbitmq = new RabbitMQContainer("rabbitmq:4.1-management");
        rabbitmq.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);

        registry.add("spring.rabbitmq.virtual-host", () -> "/");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }
}