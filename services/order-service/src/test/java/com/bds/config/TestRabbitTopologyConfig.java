package com.bds.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestRabbitTopologyConfig {

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange("payment.exchange").durable(true).build();
    }

    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange("notification.exchange").durable(true).build();
    }
}