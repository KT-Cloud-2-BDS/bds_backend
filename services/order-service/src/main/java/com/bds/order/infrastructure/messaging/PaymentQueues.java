package com.bds.order.infrastructure.messaging;

import com.bds.messaging.BdsQueues;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentQueues {
    public static final String ORDER_CREATED = "payment.order-created.queue";

    /**
     * workQueue parameter
     *  1. queue 이름
     * 2. 큐와 연결할 exchange
     * 3. routing key 조건
     * */
    @Bean
    public Declarables orderCreatedQueue() {
        return BdsQueues.workQueue(ORDER_CREATED, "order.exchange", "order.created");
    }
}
