package com.bds.order.infrastructure.messaging;

import com.bds.common.events.PublishTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DirectEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    public DirectEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    public void publish(String exchange, String routingKey, Object event) {
        log.info("publish exchange:{}, routingKey:{}",exchange,routingKey);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
    public void publish(Object event) {
        PublishTo target = event.getClass().getAnnotation(PublishTo.class);
        if (target == null) {
            throw new IllegalArgumentException(
                    event.getClass().getSimpleName() + "에 @PublishTo가 없습니다. " +
                            "direct 발행 이벤트는 @PublishTo로 목적지를 선언하세요. " +
                            "(유실 불가 이벤트라면 @Externalized + publishEvent 경로를 쓸 것)");
        }
        publish(target.exchange(), target.routingKey(), event);
    }
}
