package com.bds.chat.integration.messaging;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import com.bds.common.events.funding.FundingStatusChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@DisplayName("FundingCreatedListener 통합 테스트")
class FundingCreatedListenerIntegrationTest {

    @Autowired @Qualifier("msaRabbitTemplate") private RabbitTemplate msaRabbitTemplate;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatIntegrationTestFixture fixture;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Long CREATOR_ID = 1L;
    private static final String EXCHANGE    = "funding.exchange";
    private static final String ROUTING_KEY = "funding.status";
    private static final String DLQ         = "chat.funding-created.queue.dlq";

    @AfterEach
    void cleanup() {
        fixture.deleteAll();
    }

    private void publish(String type, Long targetId) {
        msaRabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY,
                FundingStatusChangedEvent.of(type, targetId, CREATOR_ID));
    }

    @Nested
    @DisplayName("이벤트 처리")
    class ProcessTest {

        @Test
        @DisplayName("FUNDING_START 이벤트 처리 시 채팅방이 DB에 생성된다")
        void FUNDING_START_이벤트_처리시_채팅방이_DB에_생성된다() {
            long productId = 9901L;

            publish("FUNDING_START", productId);

            await().atMost(10, TimeUnit.SECONDS)
                   .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                           "SELECT COUNT(*) FROM chat_room WHERE product_id = ? AND deleted_at IS NULL",
                           Integer.class, productId)).isEqualTo(1));
        }

        @Test
        @DisplayName("FUNDING_SUCCESS 이벤트 처리 시 채팅방이 DB에서 삭제된다")
        void FUNDING_SUCCESS_이벤트_처리시_채팅방이_DB에서_삭제된다() {
            Long roomId = chatRoomService.createFundingRoom(9902L, new FundingRoomCreateRequestDto(CREATOR_ID)).roomId();

            publish("FUNDING_SUCCESS", roomId);

            await().atMost(10, TimeUnit.SECONDS)
                   .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                           "SELECT COUNT(*) FROM chat_room WHERE id = ? AND deleted_at IS NULL",
                           Integer.class, roomId)).isZero());
        }

        @Test
        @DisplayName("FUNDING_FAIL 이벤트 처리 시 채팅방이 DB에서 삭제된다")
        void FUNDING_FAIL_이벤트_처리시_채팅방이_DB에서_삭제된다() {
            Long roomId = chatRoomService.createFundingRoom(9903L, new FundingRoomCreateRequestDto(CREATOR_ID)).roomId();

            publish("FUNDING_FAIL", roomId);

            await().atMost(10, TimeUnit.SECONDS)
                   .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                           "SELECT COUNT(*) FROM chat_room WHERE id = ? AND deleted_at IS NULL",
                           Integer.class, roomId)).isZero());
        }
    }

    @Nested
    @DisplayName("멱등성")
    class IdempotencyTest {

        @Test
        @DisplayName("같은 productId로 FUNDING_START가 두 번 오면 채팅방은 하나만 생성된다")
        void 같은_productId로_FUNDING_START가_두번_오면_채팅방은_하나만_생성된다() {
            long productId = 9904L;

            publish("FUNDING_START", productId);
            publish("FUNDING_START", productId);

            await().atMost(10, TimeUnit.SECONDS)
                   .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                           "SELECT COUNT(*) FROM chat_room WHERE product_id = ?",
                           Integer.class, productId)).isEqualTo(1));
        }

        @Test
        @DisplayName("이미 삭제된 채팅방의 FUNDING_SUCCESS가 재전달돼도 DLQ로 가지 않는다")
        void 이미_삭제된_채팅방의_FUNDING_SUCCESS가_재전달돼도_DLQ로_가지_않는다() {
            Long roomId = chatRoomService.createFundingRoom(9905L, new FundingRoomCreateRequestDto(CREATOR_ID)).roomId();
            publish("FUNDING_SUCCESS", roomId);
            await().atMost(10, TimeUnit.SECONDS)
                   .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                           "SELECT COUNT(*) FROM chat_room WHERE id = ? AND deleted_at IS NULL",
                           Integer.class, roomId)).isZero());

            publish("FUNDING_SUCCESS", roomId); // 재전달

            // BusinessException은 listener에서 삼켜지므로 DLQ에 쌓이지 않아야 함
            assertThat(msaRabbitTemplate.receive(DLQ, 3_000)).isNull();
        }

        @Test
        @DisplayName("이미 삭제된 채팅방의 FUNDING_FAIL이 재전달돼도 DLQ로 가지 않는다")
        void 이미_삭제된_채팅방의_FUNDING_FAIL이_재전달돼도_DLQ로_가지_않는다() {
            Long roomId = chatRoomService.createFundingRoom(9906L, new FundingRoomCreateRequestDto(CREATOR_ID)).roomId();
            publish("FUNDING_FAIL", roomId);
            await().atMost(10, TimeUnit.SECONDS)
                   .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                           "SELECT COUNT(*) FROM chat_room WHERE id = ? AND deleted_at IS NULL",
                           Integer.class, roomId)).isZero());

            publish("FUNDING_FAIL", roomId); // 재전달

            assertThat(msaRabbitTemplate.receive(DLQ, 3_000)).isNull();
        }
    }
}
