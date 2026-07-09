package com.bds.chat.integration.blackList;

import com.bds.chat.application.blackList.dto.BlackListCreateRequestDto;
import com.bds.chat.application.blackList.service.BlackListService;
import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.domain.blackList.FundingChatBlacklistRepository;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// 동시성 테스트는 실제 commit이 필요하므로 @Transactional 없이 운영
// @AfterEach에서 JdbcTemplate으로 직접 cleanup
@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@DisplayName("블랙리스트 동시성 테스트")
class BlackListServiceConcurrencyTest {

    @Autowired private BlackListService blackListService;
    @Autowired private FundingChatBlacklistRepository blacklistRepository;
    @Autowired private ChatIntegrationTestFixture fixture;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Long CREATOR_ID = 2L;
    private static final Long TARGET_ID = 7L;
    private static final Long PRODUCT_ID = 900L;  // 다른 테스트와 충돌 방지용 전용 ID

    private Long createdRoomId;

    @AfterEach
    void cleanup() {
        if (createdRoomId != null) {
            jdbcTemplate.update("DELETE FROM funding_chat_blacklist WHERE room_id = ?", createdRoomId);
            jdbcTemplate.update("DELETE FROM chat_room WHERE id = ?", createdRoomId);
            createdRoomId = null;
        }
    }

    // 동시 5개 차단 요청 → 정확히 1건만 성공 (중복 저장 방지)
    @Test
    @DisplayName("동시에 같은 대상을 차단하면 하나만 저장된다")
    void 동시에_같은_대상을_차단하면_하나만_저장된다() throws InterruptedException {
        ChatRoom room = fixture.createRoom("concurrency-room", PRODUCT_ID, ChatRoomType.FUNDING, ChatRoomStatus.ACTIVE, CREATOR_ID);
        createdRoomId = room.getId().value();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    ready.await();
                    blackListService.create(createdRoomId, CREATOR_ID, new BlackListCreateRequestDto(TARGET_ID, "spam"));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {}
            }));
        }

        ready.countDown();
        for (Future<?> f : futures) {
            try { f.get(); } catch (ExecutionException ignored) {}
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 정확히 1건만 ACTIVE 상태로 DB에 존재
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(blacklistRepository.isBlacklisted(createdRoomId, TARGET_ID)).isTrue();
    }
}
