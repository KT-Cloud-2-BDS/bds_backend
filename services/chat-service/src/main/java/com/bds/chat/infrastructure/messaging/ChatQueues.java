package com.bds.chat.infrastructure.messaging;
import com.bds.messaging.BdsQueues;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatQueues {
    public static final String FUNDING_CREATED = "chat.funding-created.queue";

    @Bean
    public Declarables fundingCreatedQueue(@Qualifier("msaRabbitAdmin") RabbitAdmin msaAdmin) {
        Declarables d = BdsQueues.workQueue(FUNDING_CREATED, "funding.exchange", "funding.status");
        d.getDeclarables().forEach(x -> ((Declarable) x).setAdminsThatShouldDeclare(msaAdmin));
        return d;
    }
}
