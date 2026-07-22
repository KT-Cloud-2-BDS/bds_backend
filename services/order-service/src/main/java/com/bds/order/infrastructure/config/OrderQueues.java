package com.bds.order.infrastructure.config;

import com.bds.messaging.BdsQueues;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderQueues {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    public static final String ORDER_PROCESS_SETTLE_QUEUE = "order.payment-settled.queue";
    public static final String ORDER_PROCESS_PAID_QUEUE = "order.payment-paid.queue";
    public static final String ORDER_PROCESS_REFUND_QUEUE = "order.payment-refunded.queue";

    @Bean
    public Declarables orderProcessSettlementQueue() {
        return BdsQueues.workQueue(ORDER_PROCESS_SETTLE_QUEUE, PAYMENT_EXCHANGE, "payment.settled");
    }

    @Bean
    public Declarables orderProcessPaidQueue() {
        return BdsQueues.workQueue(ORDER_PROCESS_PAID_QUEUE, PAYMENT_EXCHANGE, "payment.paid");
    }

    @Bean
    public Declarables orderProcessRefundQueue() {
        return BdsQueues.workQueue(ORDER_PROCESS_REFUND_QUEUE, PAYMENT_EXCHANGE, "payment.refunded");
    }
}

