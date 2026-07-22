package com.bds.chat.integration.messaging;

import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import com.bds.common.events.funding.FundingStatusChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@DisplayName("MSA 브로커 실제 연결 및 이벤트 consume 테스트")
class MsaBrokerConnectionIntegrationTest {

    @Autowired
    @Qualifier("msaRabbitTemplate")
    private RabbitTemplate msaRabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ChatIntegrationTestFixture fixture;

    @AfterEach
    void cleanup() {
        fixture.deleteAll();
    }

    @Test
    @DisplayName("MSA 브로커에 실제로 연결된다")
    void MSA_브로커에_실제로_연결된다() {
        assertThatCode(() ->
            msaRabbitTemplate.execute(channel -> {
                channel.basicQos(1);
                return null;
            })
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FUNDING_START 이벤트 발행 시 listener가 채팅방을 생성한다")
    void FUNDING_START_이벤트_발행시_채팅방이_생성된다() {
        long productId = 9801L;

        msaRabbitTemplate.convertAndSend(
                "funding.exchange", "funding.status",
                FundingStatusChangedEvent.of("FUNDING_START", productId, 1L));

        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   Integer count = jdbcTemplate.queryForObject(
                           "SELECT COUNT(*) FROM chat_room WHERE product_id = ? AND deleted_at IS NULL",
                           Integer.class, productId);
                   assertThat(count).isEqualTo(1);
               });
    }

    @Test
    @DisplayName("FUNDING_SUCCESS 이벤트 발행 시 listener가 채팅방을 삭제한다")
    void FUNDING_SUCCESS_이벤트_발행시_채팅방이_삭제된다() {
        long productId = 9802L;

        msaRabbitTemplate.convertAndSend(
                "funding.exchange", "funding.status",
                FundingStatusChangedEvent.of("FUNDING_START", productId, 1L));

        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                       "SELECT COUNT(*) FROM chat_room WHERE product_id = ? AND deleted_at IS NULL",
                       Integer.class, productId)).isEqualTo(1));

        Long roomId = jdbcTemplate.queryForObject(
                "SELECT id FROM chat_room WHERE product_id = ? AND deleted_at IS NULL",
                Long.class, productId);

        msaRabbitTemplate.convertAndSend(
                "funding.exchange", "funding.status",
                FundingStatusChangedEvent.of("FUNDING_SUCCESS", roomId, 1L));

        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   Integer count = jdbcTemplate.queryForObject(
                           "SELECT COUNT(*) FROM chat_room WHERE id = ? AND deleted_at IS NULL",
                           Integer.class, roomId);
                   assertThat(count).isZero();
               });
    }

    @Test
    @DisplayName("FUNDING_FAIL 이벤트 발행 시 listener가 채팅방을 삭제한다")
    void FUNDING_FAIL_이벤트_발행시_채팅방이_삭제된다() {
        long productId = 9803L;

        msaRabbitTemplate.convertAndSend(
                "funding.exchange", "funding.status",
                FundingStatusChangedEvent.of("FUNDING_START", productId, 1L));

        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                       "SELECT COUNT(*) FROM chat_room WHERE product_id = ? AND deleted_at IS NULL",
                       Integer.class, productId)).isEqualTo(1));

        Long roomId = jdbcTemplate.queryForObject(
                "SELECT id FROM chat_room WHERE product_id = ? AND deleted_at IS NULL",
                Long.class, productId);

        msaRabbitTemplate.convertAndSend(
                "funding.exchange", "funding.status",
                FundingStatusChangedEvent.of("FUNDING_FAIL", roomId, 1L));

        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   Integer count = jdbcTemplate.queryForObject(
                           "SELECT COUNT(*) FROM chat_room WHERE id = ? AND deleted_at IS NULL",
                           Integer.class, roomId);
                   assertThat(count).isZero();
               });
    }
}
