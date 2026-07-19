package com.bds.payment.payment.infrastructure.config;

import com.bds.messaging.BdsQueues;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentQueues {

    public static final String PAY_QUEUE = "payment.pay.queue";
    public static final String SETTLEMENT_QUEUE = "payment.settlement.queue";
    public static final String REFUND_QUEUE = "payment.refund.queue";

    @Bean
    public Declarables payQueue() {
        return BdsQueues.workQueue(PAY_QUEUE, "payment.exchange", "payment.process.pay");
    }

    @Bean
    public Declarables settlementQueue() {
        return BdsQueues.workQueue(SETTLEMENT_QUEUE, "payment.exchange", "payment.process.settlement");
    }

    @Bean
    public Declarables refundQueue() {
        return BdsQueues.workQueue(REFUND_QUEUE, "payment.exchange", "payment.process.refund");
    }
}