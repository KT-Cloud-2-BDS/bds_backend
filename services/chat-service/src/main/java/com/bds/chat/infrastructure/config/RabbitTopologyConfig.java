package com.bds.chat.infrastructure.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    public TopicExchange chatExchange(@Qualifier("msaRabbitAdmin") RabbitAdmin msaAdmin) {
        TopicExchange ex = ExchangeBuilder.topicExchange("notification.exchange").durable(true).build();
        ex.setAdminsThatShouldDeclare(msaAdmin);
        return ex;
    }
}
