package com.bds.chat.integration.blackList;

import com.bds.chat.application.blackList.dto.BlackListCreateRequestDto;
import com.bds.chat.application.blackList.service.BlackListService;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@Transactional
@DisplayName("블랙리스트 서비스 통합 예외 테스트")
class BlackListServiceIntegrationExceptionTest {

    @Autowired private BlackListService blackListService;
    @Autowired private ChatIntegrationTestFixture fixture;

    private static final Long CREATOR_ID = 2L;
    private static final Long TARGET_ID = 7L;
    private static final Long PRODUCT_ID = 3L;
    private static final Long UNKNOWN_ROOM_ID = 999999L;

    @Nested
    @DisplayName("블랙리스트 추가 예외")
    class CreateExceptionTest {

        // 자기 자신 차단 시도 → 비즈니스 규칙 위반 (INVALID_INPUT)
        @Test
        @DisplayName("자기 자신을 차단하면 INVALID_INPUT 예외가 발생한다")
        void 자기_자신을_차단하면_INVALID_INPUT_예외() {
            assertThatThrownBy(() ->
                    blackListService.create(UNKNOWN_ROOM_ID, CREATOR_ID, new BlackListCreateRequestDto(CREATOR_ID, null))
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }

        // 존재하지 않는 방 → 채팅방 조회 실패 (NOT_FOUND)
        @Test
        @DisplayName("존재하지 않는 방에 추가하면 NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_방에_추가하면_NOT_FOUND_예외() {
            assertThatThrownBy(() ->
                    blackListService.create(UNKNOWN_ROOM_ID, CREATOR_ID, new BlackListCreateRequestDto(TARGET_ID, null))
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        // INQUIRY 방에서 블랙리스트 기능 사용 시도 → 펀딩방 전용 기능 (FORBIDDEN)
        @Test
        @DisplayName("INQUIRY 방에 추가하면 FORBIDDEN 예외가 발생한다")
        void INQUIRY_방에_추가하면_FORBIDDEN_예외() {
            ChatRoom room = fixture.createRoom("exc-inquiry-create", PRODUCT_ID, ChatRoomType.INQUIRY, ChatRoomStatus.ACTIVE, CREATOR_ID);

            assertThatThrownBy(() ->
                    blackListService.create(room.getId().value(), CREATOR_ID, new BlackListCreateRequestDto(TARGET_ID, null))
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        // 방장이 아닌 사용자가 차단 시도 → 권한 없음 (FORBIDDEN)
        @Test
        @DisplayName("방장이 아닌 사용자가 추가하면 FORBIDDEN 예외가 발생한다")
        void 방장이_아닌_사용자가_추가하면_FORBIDDEN_예외() {
            ChatRoom room = fixture.createRoom("exc-non-creator-create", PRODUCT_ID, ChatRoomType.FUNDING, ChatRoomStatus.ACTIVE, CREATOR_ID);

            assertThatThrownBy(() ->
                    blackListService.create(room.getId().value(), 99L, new BlackListCreateRequestDto(TARGET_ID, null))
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        // 이미 ACTIVE 상태인 대상을 중복 차단 시도 → 데이터 정합성 (CONFLICT)
        @Test
        @DisplayName("이미 차단된 대상을 추가하면 CONFLICT 예외가 발생한다")
        void 이미_차단된_대상을_추가하면_CONFLICT_예외() {
            ChatRoom room = fixture.createRoom("exc-duplicate-create", PRODUCT_ID, ChatRoomType.FUNDING, ChatRoomStatus.ACTIVE, CREATOR_ID);
            Long roomId = room.getId().value();
            blackListService.create(roomId, CREATOR_ID, new BlackListCreateRequestDto(TARGET_ID, "first"));

            assertThatThrownBy(() ->
                    blackListService.create(roomId, CREATOR_ID, new BlackListCreateRequestDto(TARGET_ID, "second"))
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }

    @Nested
    @DisplayName("블랙리스트 해제 예외")
    class DeleteExceptionTest {

        // 존재하지 않는 방 → 채팅방 조회 실패 (NOT_FOUND)
        @Test
        @DisplayName("존재하지 않는 방에서 해제하면 NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_방에서_해제하면_NOT_FOUND_예외() {
            assertThatThrownBy(() ->
                    blackListService.delete(UNKNOWN_ROOM_ID, CREATOR_ID, TARGET_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        // INQUIRY 방에서 블랙리스트 해제 시도 → 펀딩방 전용 기능 (FORBIDDEN)
        @Test
        @DisplayName("INQUIRY 방에서 해제하면 FORBIDDEN 예외가 발생한다")
        void INQUIRY_방에서_해제하면_FORBIDDEN_예외() {
            ChatRoom room = fixture.createRoom("exc-inquiry-delete", PRODUCT_ID, ChatRoomType.INQUIRY, ChatRoomStatus.ACTIVE, CREATOR_ID);

            assertThatThrownBy(() ->
                    blackListService.delete(room.getId().value(), CREATOR_ID, TARGET_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        // 방장이 아닌 사용자가 해제 시도 → 권한 없음 (FORBIDDEN)
        @Test
        @DisplayName("방장이 아닌 사용자가 해제하면 FORBIDDEN 예외가 발생한다")
        void 방장이_아닌_사용자가_해제하면_FORBIDDEN_예외() {
            ChatRoom room = fixture.createRoom("exc-non-creator-delete", PRODUCT_ID, ChatRoomType.FUNDING, ChatRoomStatus.ACTIVE, CREATOR_ID);

            assertThatThrownBy(() ->
                    blackListService.delete(room.getId().value(), 99L, TARGET_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        // 차단 기록이 없는 대상 해제 시도 → 조회 실패 (NOT_FOUND)
        @Test
        @DisplayName("활성 차단 기록이 없으면 NOT_FOUND 예외가 발생한다")
        void 활성_차단이_없으면_NOT_FOUND_예외() {
            ChatRoom room = fixture.createRoom("exc-not-blacklisted", PRODUCT_ID, ChatRoomType.FUNDING, ChatRoomStatus.ACTIVE, CREATOR_ID);

            assertThatThrownBy(() ->
                    blackListService.delete(room.getId().value(), CREATOR_ID, TARGET_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }
}
