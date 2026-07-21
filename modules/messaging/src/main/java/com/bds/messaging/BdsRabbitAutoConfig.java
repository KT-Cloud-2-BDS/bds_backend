package com.bds.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;


/*
 *발행 실패를 침묵시키지 않기 위한 상시 감시 장치
 * 현재 감지 시 대응 수단이 로그 하나뿐이며, TODO는 그 대응 수단을 메트릭/알람으로 확장
 */
@Slf4j
@AutoConfiguration
public class BdsRabbitAutoConfig {
    @Bean
    @ConditionalOnMissingBean
    public MessageConverter bdsMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /** 회사 표준 발행 안전장치: mandatory + confirm/returns logging */
    @Bean
    public RabbitTemplateCustomizer bdsRabbitTemplateCustomizer() {
        return (RabbitTemplate template) -> {
            template.setMandatory(true);

            // publisher-confirm-type: correlated 인 서비스에서 동작 (order 등 발행 서비스 yml 참고)
            template.setConfirmCallback((correlation, ack, cause) -> {
                if (!ack) {
                    // nack: Modulith 레지스트리에 미완료로 남아 재발행 스케줄러가 회수한다.
                    // TODO 메트릭 카운터 연동 (예: bds.messaging.publish.nack) -> 모니터링 구축 스프린트
                    log.error("[bds-messaging] publish NACK. correlation={}, cause={}", correlation, cause);
                }
            });

            // exchange에는 도달했으나 라우팅될 큐가 없는 경우 (바인딩 누락 신호)
            template.setReturnsCallback(returned ->
                    log.error("[bds-messaging] message RETURNED. exchange={}, routingKey={}, replyText={}",
                            returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        };
    }
}
