package com.bds.chat.integration.message;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.message.dto.MessageSendRequestDto;
import com.bds.chat.application.message.service.MessageService;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.domain.message.ChatMessageRepository;
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
@DisplayName("메시지 서비스 동시성 테스트")
class MessageServiceConcurrencyTest {

    @Autowired private MessageService messageService;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Long SENDER_ID = 7L;
    private static final Long SELLER_ID = 2L;
    private static final Long PRODUCT_ID = 802L;  // 다른 테스트와 충돌 방지용 전용 ID

    private Long createdRoomId;

    @AfterEach
    void cleanup() {
        if (createdRoomId != null) {
            jdbcTemplate.update("DELETE FROM chat_message WHERE room_id = ?", createdRoomId);
            jdbcTemplate.update("DELETE FROM chat_room WHERE id = ?", createdRoomId);
            createdRoomId = null;
        }
    }

    // 동시 5개 메시지 전송 → 모두 성공하고 5건이 DB에 저장됨 (데이터 손실 없음)
    @Test
    @DisplayName("동시에 펀딩 채팅방에 메시지를 전송해도 모두 저장된다")
    void 동시에_펀딩_채팅방에_메시지를_전송해도_모두_저장된다() throws InterruptedException {
        createdRoomId = chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID)).roomId();
        final Long roomId = createdRoomId;

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    ready.await();
                    messageService.create(
                            new MessageSendRequestDto(roomId, "메시지 " + idx, "TEXT", null),
                            SENDER_ID
                    );
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

        // 모든 요청이 성공하고 데이터 손실 없이 5건 저장
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(chatMessageRepository.findByRoomIdBefore(roomId, null, 10)).hasSize(threadCount);
    }
}
