package com.bds.messaging;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;

public final class BdsRabbit {

    private BdsRabbit() {}

    public static RabbitTemplate standardTemplate(ConnectionFactory cf,
                                                  MessageConverter converter,
                                                  ObjectProvider<RabbitTemplateCustomizer> customizers) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        customizers.orderedStream().forEach(c -> c.customize(t));
        return t;
    }

    public static SimpleRabbitListenerContainerFactory standardListenerFactory(
            ConnectionFactory cf, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(converter);
        f.setDefaultRequeueRejected(true);
        f.setPrefetchCount(10);
        return f;
    }
}
