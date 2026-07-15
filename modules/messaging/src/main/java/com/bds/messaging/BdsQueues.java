package com.bds.messaging;

import org.springframework.amqp.core.*;

public class BdsQueues {
    public static final String DLX = "dlx.exchange";
    private static final int DELIVERY_LIMIT = 5;

    private BdsQueues() {}

    /**
     * 표준 작업 큐 세트(메인 큐 + DLQ + 바인딩 일체)를 선언한다.
     *
     * @param queueName  예: "payment.order-created.queue"
     * @param exchange   바인딩할 topic exchange (프로듀서 소유. 여기서는 참조 선언만)
     * @param routingKey 예: "order.created"
     */
    public static Declarables workQueue(String queueName, String exchange, String routingKey) {
        String deadRoutingKey = queueName + ".dead";

        Queue queue = QueueBuilder.durable(queueName)
                .quorum()
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(deadRoutingKey)
                .withArgument("x-dead-letter-strategy", "at-least-once")
                .withArgument("x-overflow", "reject-publish")
                .withArgument("x-delivery-limit", DELIVERY_LIMIT)
                .build();

        TopicExchange sourceExchange = ExchangeBuilder.topicExchange(exchange).durable(true).build();
        Binding mainBinding = BindingBuilder.bind(queue).to(sourceExchange).with(routingKey);

        DirectExchange dlx = ExchangeBuilder.directExchange(DLX).durable(true).build();
        Queue dlq = QueueBuilder.durable(queueName + ".dlq").quorum().build();
        Binding dlqBinding = BindingBuilder.bind(dlq).to(dlx).with(deadRoutingKey);

        return new Declarables(queue, sourceExchange, mainBinding, dlx, dlq, dlqBinding);
    }
}
