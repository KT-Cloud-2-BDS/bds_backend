package com.bds.chat.config;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
                .withDatabaseName("chat")
                .withUsername("postgres")
                .withPassword("postgres")
                .withReuse(true);
    }

    @Bean
    @ServiceConnection
    public RabbitMQContainer rabbitmqContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management"))
                // STOMP 포트(61613) 및 Management UI 포트(15672) 오픈
                .withExposedPorts(5672, 15672, 61613)
                // 로컬의 rabbitmq 설정 파일들을 컨테이너 내부 경로로 마운트 (볼륨 바인딩 대체)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("docker/rabbitmq/enabled_plugins"),
                        "/etc/rabbitmq/enabled_plugins"
                )
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("docker/rabbitmq/rabbitmq.conf"),
                        "/etc/rabbitmq/rabbitmq.conf"
                )
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("docker/rabbitmq/definitions.json"),
                        "/etc/rabbitmq/definitions.json"
                )
                .withReuse(true);
    }
}