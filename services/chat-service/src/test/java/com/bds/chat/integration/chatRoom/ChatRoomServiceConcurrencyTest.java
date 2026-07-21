package com.bds.chat.integration.chatRoom;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
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
@DisplayName("채팅방 서비스 동시성 테스트")
class ChatRoomServiceConcurrencyTest {

    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Long SELLER_ID = 2L;
    private static final Long PRODUCT_ID = 800L;  // 다른 테스트와 충돌 방지용 전용 ID

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM chat_room WHERE product_id = ? AND type = 'FUNDING'", PRODUCT_ID);
    }

    // 동시 5개 생성 요청 → 정확히 1건만 성공 (중복 방 방지)
    @Test
    @DisplayName("동시에 같은 상품의 펀딩 채팅방을 생성하면 하나만 저장된다")
    void 동시에_같은_상품의_펀딩_채팅방을_생성하면_하나만_저장된다() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    ready.await();
                    chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
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
        assertThat(chatRoomRepository.findFundingRoomByProduct(PRODUCT_ID)).isPresent();
    }
}
