package com.bds.payment.support;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestPaymentRabbitTopologyConfig {

    // Order가 발행하고 Payment가 구독하는 익스체인지 (테스트에서 발행자 역할을 대신함)
    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange("order.exchange").durable(true).build();
    }

    // Payment가 발행하는 결과 통보용 익스체인지 (필요 시 결과 발행 검증에도 사용)
    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange("payment.exchange").durable(true).build();
    }
}