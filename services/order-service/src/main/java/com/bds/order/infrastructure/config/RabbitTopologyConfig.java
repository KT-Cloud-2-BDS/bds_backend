package com.bds.order.infrastructure.config;

import com.bds.messaging.BdsRabbit;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";

    public static final String ORDER_PROCESS_QUEUE = "order.process";
    public static final String ORDER_PROCESS_PAID_QUEUE = "order.process.paid";
    public static final String ORDER_PROCESS_CANCEL_QUEUE = "order.process.cancel";

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
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderProcessQueue() {
        return new Queue(ORDER_PROCESS_QUEUE, true);
    }

    @Bean
    public Queue orderProcessPaidQueue() {
        return new Queue(ORDER_PROCESS_PAID_QUEUE, true);
    }

    @Bean
    public Queue orderProcessCancelQueue() {
        return new Queue(ORDER_PROCESS_CANCEL_QUEUE, true);
    }

    @Bean
    public Binding orderProcessBinding(Queue orderProcessQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderProcessQueue)
                .to(orderExchange)
                .with("order.process");
    }

    @Bean
    public Binding orderProcessPaidBinding(Queue orderProcessPaidQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderProcessPaidQueue)
                .to(orderExchange)
                .with("order.process.paid");
    }

    @Bean
    public Binding orderProcessCancelBinding(Queue orderProcessCancelQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderProcessCancelQueue)
                .to(orderExchange)
                .with("order.process.cancel");
    }


}
