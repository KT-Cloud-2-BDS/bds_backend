package com.bds.chat.infrastructure.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

@Configuration
@EnableConfigurationProperties(MsaBrokerProperties.class)
public class DualBrokerConfig {

    // ===== 내부(채팅) 브로커 — converter 미지정 = 기존 SimpleMessageConverter 동작 보존 =====

    @Bean
    @Primary
    public CachingConnectionFactory connectionFactory(RabbitProperties props) {
        CachingConnectionFactory cf = new CachingConnectionFactory(props.getHost(), props.getPort());
        cf.setUsername(props.getUsername());
        cf.setPassword(props.getPassword());
        if (props.getVirtualHost() != null) cf.setVirtualHost(props.getVirtualHost());
        return cf;
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);          // converter 파라미터 제거
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            CachingConnectionFactory connectionFactory) {       // converter 파라미터 제거
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(connectionFactory);
        return f;
    }

    // ===== MSA 공용 브로커 — JSON (Jackson 3) =====

    @Bean
    public MessageConverter msaMessageConverter() {
        return new JacksonJsonMessageConverter();               // Jackson2... 아님
    }

    @Bean
    public CachingConnectionFactory msaConnectionFactory(MsaBrokerProperties props) {
        CachingConnectionFactory cf = new CachingConnectionFactory(props.getHost(), props.getPort());
        cf.setUsername(props.getUsername());
        cf.setPassword(props.getPassword());
        cf.setVirtualHost(props.getVirtualHost());
        cf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        cf.setPublisherReturns(true);
        return cf;
    }

    @Bean
    public RabbitTemplate msaRabbitTemplate(
            @Qualifier("msaConnectionFactory") CachingConnectionFactory cf,
            @Qualifier("msaMessageConverter") MessageConverter converter,
            ObjectProvider<RabbitTemplateCustomizer> customizers) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        customizers.orderedStream().forEach(c -> c.customize(t));
        return t;
    }

    @Bean
    public RabbitAdmin msaRabbitAdmin(
            @Qualifier("msaConnectionFactory") CachingConnectionFactory cf) {
        return new RabbitAdmin(cf);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory msaListenerContainerFactory(
            @Qualifier("msaConnectionFactory") CachingConnectionFactory cf,
            @Qualifier("msaMessageConverter") MessageConverter converter) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(converter);
        f.setDefaultRequeueRejected(true);
        f.setPrefetchCount(10);
        return f;
    }
}