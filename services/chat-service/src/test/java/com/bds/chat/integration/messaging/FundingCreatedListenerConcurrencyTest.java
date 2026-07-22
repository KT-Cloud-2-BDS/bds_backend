package com.bds.chat.integration.messaging;

import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.infrastructure.messaging.FundingCreatedListener;
import com.bds.common.events.funding.FundingCreatedEvent;
import com.bds.common.events.funding.FundingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// 동시성 테스트는 실제 commit이 필요하므로 @Transactional 없이 운영
// @AfterEach에서 fixture.deleteAll()로 직접 cleanup
@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@DisplayName("FundingCreatedListener 동시성 테스트")
class FundingCreatedListenerConcurrencyTest {

    @Autowired private FundingCreatedListener listener;
    @Autowired private ChatIntegrationTestFixture fixture;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Long CREATOR_ID = 1L;
    private static final int THREAD_COUNT = 5;

    @AfterEach
    void cleanup() {
        fixture.deleteAll();
    }

    @Test
    @DisplayName("동시에 같은 productId의 FUNDING_START 이벤트가 오면 채팅방이 하나만 생성된다")
    void 동시에_같은_productId의_FUNDING_START_이벤트가_오면_채팅방이_하나만_생성된다() throws InterruptedException {
        long productId = 9910L;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> {
                try {
                    ready.await();
                    listener.handle(new FundingCreatedEvent(
                            UUID.randomUUID(), FundingType.FUNDING_START, "PRODUCT", productId, CREATOR_ID));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        ready.countDown();
        for (Future<?> f : futures) {
            try { f.get(); } catch (ExecutionException ignored) {}
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_room WHERE product_id = ?",
                Integer.class, productId);
        assertThat(count).isEqualTo(1);
        assertThat(successCount.get() + failCount.get()).isEqualTo(THREAD_COUNT);
    }

    @Test
    @DisplayName("서로 다른 productId의 FUNDING_START 이벤트를 동시에 처리하면 모두 생성된다")
    void 서로_다른_productId의_FUNDING_START_이벤트를_동시에_처리하면_모두_생성된다() throws InterruptedException {
        long baseProductId = 9920L;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            final long productId = baseProductId + i;
            futures.add(executor.submit(() -> {
                try {
                    ready.await();
                    listener.handle(new FundingCreatedEvent(
                            UUID.randomUUID(), FundingType.FUNDING_START, "PRODUCT", productId, CREATOR_ID));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {}
            }));
        }

        ready.countDown();
        for (Future<?> f : futures) {
            try { f.get(); } catch (ExecutionException ignored) {}
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_room WHERE product_id BETWEEN ? AND ?",
                Integer.class, baseProductId, baseProductId + THREAD_COUNT - 1);
        assertThat(count).isEqualTo(THREAD_COUNT);
    }
}
