package com.bds.notification.infrastructure.messaging;

import com.bds.messaging.BdsQueues;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

  // Exchange 상수
  public static final String ORDER_EXCHANGE = "order.exchange";
  public static final String FUNDING_EXCHANGE = "funding.exchange";
  public static final String CHATTING_EXCHANGE = "chatting.exchange";

  // 라우팅 키
  public static final String ORDER_STATUS_ROUTING_KEY = "order.status";
  public static final String FUNDING_STATUS_ROUTING_KEY = "funding.status";
  public static final String CHATTING_STATUS_ROUTING_KEY = "chatting.status";

  // 큐
  public static final String ORDER_STATUS_QUEUE = "notification.order-status.queue";
  public static final String FUNDING_STATUS_QUEUE = "notification.funding-status.queue";
  public static final String CHATTING_STATUS_QUEUE = "notification.chatting-status.queue";

  // 임시 Exchange 선언
  // 추후 Order Funding 쪽에서 아마도 만들어서 줄 것이기때문에.. 임시로 테스트 용으로 작성함
  @Bean
  public TopicExchange orderExchange() {
    return ExchangeBuilder.topicExchange(ORDER_EXCHANGE).durable(true).build();
  }

  @Bean
  public TopicExchange fundingExchange() {
    return ExchangeBuilder.topicExchange(FUNDING_EXCHANGE).durable(true).build();
  }

  @Bean
  public TopicExchange chattingExchange() {
    return ExchangeBuilder.topicExchange(CHATTING_EXCHANGE).durable(true).build();
  }

  // Queue 선언
  @Bean
  public Declarables orderStatusQueue() {
    return BdsQueues.workQueue(
        ORDER_STATUS_QUEUE,
        ORDER_EXCHANGE,
        ORDER_STATUS_ROUTING_KEY
    );
  }

  @Bean
  public Declarables fundingStatusQueue() {
    return BdsQueues.workQueue(
        FUNDING_STATUS_QUEUE,
        FUNDING_EXCHANGE,
        FUNDING_STATUS_ROUTING_KEY
    );
  }

  @Bean
  public Declarables chattingStatusQueue() {
    return BdsQueues.workQueue(
        CHATTING_STATUS_QUEUE,
        CHATTING_EXCHANGE,
        CHATTING_STATUS_ROUTING_KEY
    );
  }
}
