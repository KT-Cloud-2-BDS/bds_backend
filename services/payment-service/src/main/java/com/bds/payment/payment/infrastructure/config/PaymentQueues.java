package com.bds.payment.payment.infrastructure.config;

import com.bds.messaging.BdsQueues;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentQueues {

    public static final String PAY_QUEUE = "payment.order.pay.queue";
    public static final String SETTLEMENT_QUEUE = "payment.order.settlement.queue";
    public static final String REFUND_QUEUE = "payment.order.refund.queue";

    public static final String ORDER_EXCHANGE = "order.exchange";

    @Bean
    public Declarables payQueue() {
        return BdsQueues.workQueue(PAY_QUEUE, ORDER_EXCHANGE, "order.pay.requested");
    }

    @Bean
    public Declarables settlementQueue() {
        return BdsQueues.workQueue(SETTLEMENT_QUEUE, ORDER_EXCHANGE, "order.settle.requested");
    }

    @Bean
    public Declarables refundQueue() {
        return BdsQueues.workQueue(REFUND_QUEUE, ORDER_EXCHANGE, "order.refund.requested");
    }
}