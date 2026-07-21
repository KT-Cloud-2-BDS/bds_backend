package com.bds.chat.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class DirectEventPublisherUnitExceptionTest {

    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks DirectEventPublisher publisher;

    record NoAnnotationEvent(String data) {}

    @Nested
    @DisplayName("발행 예외")
    class PublishExceptionTest {

        @Test
        void PublishTo_어노테이션이_없는_이벤트는_IllegalArgumentException을_던진다() {
            NoAnnotationEvent event = new NoAnnotationEvent("data");

            assertThatThrownBy(() -> publisher.publish(event))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("@PublishTo");
        }

        @Test
        void rabbitTemplate_오류시_예외가_호출자로_전파된다() {
            doThrow(new RuntimeException("연결 실패"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

            assertThatThrownBy(() -> publisher.publish("ex", "key", "payload"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("연결 실패");
        }
    }
}
