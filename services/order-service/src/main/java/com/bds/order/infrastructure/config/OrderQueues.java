package com.bds.order.infrastructure.config;

import com.bds.messaging.BdsQueues;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderQueues {

    public static final String ORDER_EXCHANGE = "order.exchange";

    public static final String ORDER_PROCESS_QUEUE = "order.process";
    public static final String ORDER_PROCESS_PAID_QUEUE = "order.process.paid";
    public static final String ORDER_PROCESS_CANCEL_QUEUE = "order.process.cancel";

    @Bean
    public Declarables orderProcessQueue() {
        return BdsQueues.workQueue(ORDER_PROCESS_QUEUE, ORDER_EXCHANGE, "order.process");
    }

    @Bean
    public Declarables orderProcessPaidQueue() {
        return BdsQueues.workQueue(ORDER_PROCESS_PAID_QUEUE, ORDER_EXCHANGE, "order.process.paid");
    }

    @Bean
    public Declarables orderProcessCancelQueue() {
        return BdsQueues.workQueue(ORDER_PROCESS_CANCEL_QUEUE, ORDER_EXCHANGE, "order.process.cancel");
    }
}

