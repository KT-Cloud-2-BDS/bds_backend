package com.bds.order.infrastructure.config;

import com.bds.messaging.BdsRabbit;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.Externalized;

@Configuration
public class RabbitTopologyConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String FUNDING_EXCHANGE = "funding.exchange";

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf,
                                         MessageConverter converter,
                                         ObjectProvider<RabbitTemplateCustomizer> customizers) {
        return BdsRabbit.standardTemplate(cf, converter, customizers);
    }

    @Bean
    public EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .selectAndRoute(Externalized.class, Externalized::value)
                .build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange fundingExchange() {
        return ExchangeBuilder.topicExchange(FUNDING_EXCHANGE).durable(true).build();
    }

}
