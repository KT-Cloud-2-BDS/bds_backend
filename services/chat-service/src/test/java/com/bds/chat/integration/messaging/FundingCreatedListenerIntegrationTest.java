package com.bds.chat.integration.messaging;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.infrastructure.messaging.FundingCreatedListener;
import com.bds.common.events.funding.FundingCreatedEvent;
import com.bds.common.events.funding.FundingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@DisplayName("FundingCreatedListener 통합 테스트")
class FundingCreatedListenerIntegrationTest {

    @Autowired private FundingCreatedListener listener;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatIntegrationTestFixture fixture;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Long CREATOR_ID = 1L;

    @AfterEach
    void cleanup() {
        fixture.deleteAll();
    }

    private FundingCreatedEvent event(FundingType type, Long targetId) {
        return new FundingCreatedEvent(UUID.randomUUID(), type, "PRODUCT", targetId, CREATOR_ID);
    }

    @Nested
    @DisplayName("이벤트 처리")
    class ProcessTest {

        @Test
        @DisplayName("FUNDING_START 이벤트 처리 시 채팅방이 DB에 생성된다")
        void FUNDING_START_이벤트_처리시_채팅방이_DB에_생성된다() {
            long productId = 9901L;

            listener.handle(event(FundingType.FUNDING_START, productId));

            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM chat_room WHERE product_id = ? AND deleted_at IS NULL",
                    Integer.class, productId);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("FUNDING_SUCCESS 이벤트 처리 시 채팅방이 DB에서 삭제된다")
        void FUNDING_SUCCESS_이벤트_처리시_채팅방이_DB에서_삭제된다() {
            Long roomId = chatRoomService.createFundingRoom(9902L, new FundingRoomCreateRequestDto(CREATOR_ID)).roomId();

            listener.handle(event(FundingType.FUNDING_SUCCESS, roomId));

            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM chat_room WHERE id = ? AND deleted_at IS NULL",
                    Integer.class, roomId);
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("FUNDING_FAIL 이벤트 처리 시 채팅방이 DB에서 삭제된다")
        void FUNDING_FAIL_이벤트_처리시_채팅방이_DB에서_삭제된다() {
            Long roomId = chatRoomService.createFundingRoom(9903L, new FundingRoomCreateRequestDto(CREATOR_ID)).roomId();

            listener.handle(event(FundingType.FUNDING_FAIL, roomId));

            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM chat_room WHERE id = ? AND deleted_at IS NULL",
                    Integer.class, roomId);
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("멱등성")
    class IdempotencyTest {

        @Test
        @DisplayName("같은 productId로 FUNDING_START가 두 번 오면 채팅방은 하나만 생성된다")
        void 같은_productId로_FUNDING_START가_두번_오면_채팅방은_하나만_생성된다() {
            long productId = 9904L;

            listener.handle(event(FundingType.FUNDING_START, productId));
            assertThatCode(() -> listener.handle(event(FundingType.FUNDING_START, productId)))
                    .doesNotThrowAnyException();

            int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM chat_room WHERE product_id = ?",
                    Integer.class, productId);
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 삭제된 채팅방의 FUNDING_SUCCESS가 재전달돼도 예외 없이 처리된다")
        void 이미_삭제된_채팅방의_FUNDING_SUCCESS가_재전달돼도_예외_없이_처리된다() {
            Long roomId = chatRoomService.createFundingRoom(9905L, new FundingRoomCreateRequestDto(CREATOR_ID)).roomId();
            FundingCreatedEvent successEvent = event(FundingType.FUNDING_SUCCESS, roomId);

            listener.handle(successEvent);
            assertThatCode(() -> listener.handle(successEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 삭제된 채팅방의 FUNDING_FAIL이 재전달돼도 예외 없이 처리된다")
        void 이미_삭제된_채팅방의_FUNDING_FAIL이_재전달돼도_예외_없이_처리된다() {
            Long roomId = chatRoomService.createFundingRoom(9906L, new FundingRoomCreateRequestDto(CREATOR_ID)).roomId();
            FundingCreatedEvent failEvent = event(FundingType.FUNDING_FAIL, roomId);

            listener.handle(failEvent);
            assertThatCode(() -> listener.handle(failEvent))
                    .doesNotThrowAnyException();
        }
    }
}
