package com.bds.chat.integration.message;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.message.dto.MessageSendRequestDto;
import com.bds.chat.application.message.service.MessageService;
import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@DisplayName("메시지 전송 시 MSA 브로커 ChatSendEvent 발행 통합 테스트")
class MessageEventPublishIntegrationTest {

    private static final String CAPTURE_QUEUE = "test.chat-send.capture";

    private static final Long SELLER_ID  = 2L;
    private static final Long BUYER_ID   = 7L;
    private static final Long PRODUCT_ID = 20L;

    @Autowired @Qualifier("msaRabbitTemplate") private RabbitTemplate msaRabbitTemplate;
    @Autowired @Qualifier("msaRabbitAdmin")    private RabbitAdmin msaRabbitAdmin;
    @Autowired private MessageService messageService;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatIntegrationTestFixture fixture;

    @BeforeEach
    void setupCaptureQueue() {
        TopicExchange chatExchange = new TopicExchange("chat.exchange", true, false);
        Queue captureQueue = new Queue(CAPTURE_QUEUE, false, true, true); // exclusive=true
        Binding binding = BindingBuilder.bind(captureQueue).to(chatExchange).with("chat.send");

        msaRabbitAdmin.declareExchange(chatExchange);
        msaRabbitAdmin.declareQueue(captureQueue);
        msaRabbitAdmin.declareBinding(binding);
    }

    @AfterEach
    void cleanup() {
        msaRabbitAdmin.deleteQueue(CAPTURE_QUEUE);
        fixture.deleteAll();
    }

    private String receiveBody() {
        Message raw = msaRabbitTemplate.receive(CAPTURE_QUEUE, 1_000);
        if (raw == null) return null;
        return new String(raw.getBody());
    }

    @Nested
    @DisplayName("문의방 메시지 전송")
    class InquiryRoomPublishTest {

        @Test
        @DisplayName("문의방에 메시지 전송 시 ChatSendEvent가 MSA 브로커에 발행된다")
        void 문의방_메시지_전송시_ChatSendEvent가_MSA_브로커에_발행된다() {
            chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
            Long inquiryRoomId = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID).roomId();

            messageService.create(
                    new MessageSendRequestDto(inquiryRoomId, "테스트 메시지", "TEXT", null),
                    BUYER_ID);

            await().atMost(10, TimeUnit.SECONDS)
                   .untilAsserted(() -> {
                       String body = receiveBody();
                       assertThat(body).isNotNull();
                       assertThat(body).contains("\"roomId\":" + inquiryRoomId);
                       assertThat(body).contains("\"memberId\":" + SELLER_ID); // 수신자 = seller
                       assertThat(body).contains("테스트 메시지");
                   });
        }

        @Test
        @DisplayName("발신자가 바뀌어도 상대방에게 이벤트가 발행된다")
        void 발신자가_바뀌어도_상대방에게_이벤트가_발행된다() {
            chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
            Long inquiryRoomId = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID).roomId();

            messageService.create(
                    new MessageSendRequestDto(inquiryRoomId, "판매자 메시지", "TEXT", null),
                    SELLER_ID);

            await().atMost(10, TimeUnit.SECONDS)
                   .untilAsserted(() -> {
                       String body = receiveBody();
                       assertThat(body).isNotNull();
                       assertThat(body).contains("\"memberId\":" + BUYER_ID); // 수신자 = buyer
                   });
        }
    }

    @Nested
    @DisplayName("펀딩방 메시지 전송")
    class FundingRoomPublishTest {

        @Test
        @DisplayName("펀딩방에 메시지 전송 시 ChatSendEvent가 발행되지 않는다")
        void 펀딩방_메시지_전송시_ChatSendEvent가_발행되지_않는다() {
            Long fundingRoomId = chatRoomService
                    .createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID))
                    .roomId();

            messageService.create(
                    new MessageSendRequestDto(fundingRoomId, "펀딩 메시지", "TEXT", null),
                    BUYER_ID);

            // 2초간 큐가 비어 있어야 함
            Message raw = msaRabbitTemplate.receive(CAPTURE_QUEUE, 2_000);
            assertThat(raw).isNull();
        }
    }
}
