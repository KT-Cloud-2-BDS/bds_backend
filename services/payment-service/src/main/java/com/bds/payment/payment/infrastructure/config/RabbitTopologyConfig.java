package com.bds.payment.payment.infrastructure.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.Externalized;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange("payment.exchange").durable(true).build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange("order.exchange").durable(true).build();
    }

    @Bean
    public EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .selectAndRoute(Externalized.class, Externalized::value)
                .build();
    }
}
