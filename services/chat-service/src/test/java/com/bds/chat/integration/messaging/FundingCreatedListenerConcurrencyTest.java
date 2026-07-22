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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// 동시성 테스트는 실제 commit이 필요하므로 @Transactional 없이 운영
// @AfterEach에서 fixture.deleteAll()로 직접 cleanup
@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@DisplayName("FundingCreatedListener 동시성 테스트")
class FundingCreatedListenerConcurrencyTest {

    @Autowired @Qualifier("msaRabbitTemplate") private RabbitTemplate msaRabbitTemplate;
    @Autowired private ChatIntegrationTestFixture fixture;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Long CREATOR_ID  = 1L;
    private static final int  MSG_COUNT   = 5;
    private static final String EXCHANGE    = "funding.exchange";
    private static final String ROUTING_KEY = "funding.status";

    @AfterEach
    void cleanup() {
        fixture.deleteAll();
    }

    private void publish(String type, long targetId) {
        msaRabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY,
                FundingStatusChangedEvent.of(type, targetId, CREATOR_ID));
    }

    @Test
    @DisplayName("동시에 같은 productId의 FUNDING_START 이벤트가 오면 채팅방이 하나만 생성된다")
    void 동시에_같은_productId의_FUNDING_START_이벤트가_오면_채팅방이_하나만_생성된다() {
        long productId = 9910L;

        // 동일 productId로 메시지를 연속 발행 — listener가 중복 처리해도 DB는 1건이어야 함
        IntStream.range(0, MSG_COUNT).forEach(i -> publish("FUNDING_START", productId));

        await().atMost(15, TimeUnit.SECONDS)
               .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                       "SELECT COUNT(*) FROM chat_room WHERE product_id = ?",
                       Integer.class, productId)).isEqualTo(1));
    }

    @Test
    @DisplayName("서로 다른 productId의 FUNDING_START 이벤트를 동시에 처리하면 모두 생성된다")
    void 서로_다른_productId의_FUNDING_START_이벤트를_동시에_처리하면_모두_생성된다() {
        long baseProductId = 9920L;

        IntStream.range(0, MSG_COUNT).forEach(i -> publish("FUNDING_START", baseProductId + i));

        await().atMost(15, TimeUnit.SECONDS)
               .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                       "SELECT COUNT(*) FROM chat_room WHERE product_id BETWEEN ? AND ?",
                       Integer.class, baseProductId, baseProductId + MSG_COUNT - 1)).isEqualTo(MSG_COUNT));
    }
}
