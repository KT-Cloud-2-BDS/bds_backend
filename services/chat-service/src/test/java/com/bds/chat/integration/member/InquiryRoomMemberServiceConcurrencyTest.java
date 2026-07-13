package com.bds.chat.integration.member;

import com.bds.chat.application.chatRoom.dto.ChatRoomResponseDto;
import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.member.service.InquiryRoomMemberService;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
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
@DisplayName("문의 채팅방 멤버 서비스 동시성 테스트")
class InquiryRoomMemberServiceConcurrencyTest {

    @Autowired private InquiryRoomMemberService memberService;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private InquiryChatMemberRepository memberRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Long SELLER_ID = 2L;
    private static final Long BUYER_ID = 7L;
    private static final Long PRODUCT_ID = 801L;  // 다른 테스트와 충돌 방지용 전용 ID

    private Long createdInquiryRoomId;

    @AfterEach
    void cleanup() {
        if (createdInquiryRoomId != null) {
            jdbcTemplate.update("DELETE FROM inquiry_chat_member WHERE room_id = ?", createdInquiryRoomId);
            jdbcTemplate.update("DELETE FROM chat_room WHERE id = ?", createdInquiryRoomId);
        }
        jdbcTemplate.update("DELETE FROM chat_room WHERE product_id = ? AND type = 'FUNDING'", PRODUCT_ID);
        createdInquiryRoomId = null;
    }

    // 동시 5개 나가기 요청 → 최소 1건 성공, 최종 상태는 나감 (ACTIVE 아님)
    @Test
    @DisplayName("동시에 같은 멤버가 나가기를 요청해도 최종 상태는 나감이 된다")
    void 동시에_같은_멤버가_나가기를_요청해도_최종_상태는_나감이_된다() throws InterruptedException {
        chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
        ChatRoomResponseDto inquiry = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);
        createdInquiryRoomId = inquiry.roomId();
        final Long roomId = createdInquiryRoomId;

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    ready.await();
                    memberService.leave(roomId, BUYER_ID);
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

        // 최소 1건 성공, 최종 상태는 ACTIVE 아님
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(memberRepository.findActiveMember(roomId, BUYER_ID)).isEmpty();
    }
}
