package com.bds.chat.infrastructure.messaging;

import com.bds.common.events.chat.ChatSendEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DirectEventPublisherUnitTest {

    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks DirectEventPublisher publisher;

    @Nested
    @DisplayName("직접 발행")
    class DirectPublishTest {

        @Test
        void exchange와_routingKey를_직접_지정하여_발행한다() {
            Object payload = new Object();

            publisher.publish("test.exchange", "test.key", payload);

            verify(rabbitTemplate).convertAndSend("test.exchange", "test.key", payload);
        }

        @Test
        void PublishTo_어노테이션의_exchange와_routingKey로_자동_발행한다() {
            ChatSendEvent event = ChatSendEvent.of(1L, 2L, "hello");

            publisher.publish(event);

            verify(rabbitTemplate).convertAndSend("chat.exchange", "chat.send", event);
        }
    }
}
