package com.bds.chat.integration.blackList;

import com.bds.chat.application.blackList.dto.BlackListCreateRequestDto;
import com.bds.chat.application.blackList.dto.BlackListReponseDto;
import com.bds.chat.application.blackList.service.BlackListService;
import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.domain.blackList.FundingChatBlacklist;
import com.bds.chat.domain.blackList.FundingChatBlacklistRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@Transactional
@DisplayName("블랙리스트 서비스 통합 테스트")
class BlackListServiceIntegrationTest {

    @Autowired private BlackListService blackListService;
    @Autowired private FundingChatBlacklistRepository blacklistRepository;
    @Autowired private ChatIntegrationTestFixture fixture;

    private static final Long CREATOR_ID = 2L;
    private static final Long TARGET_ID = 7L;
    private static final Long PRODUCT_ID = 3L;

    @Nested
    @DisplayName("블랙리스트 추가")
    class CreateTest {

        // 정상 추가 → 응답 DTO 및 DB 저장 상태 검증
        @Test
        @DisplayName("블랙리스트 추가 시 DB에 저장된다")
        void 성공적으로_블랙리스트를_추가되어_DB에_저장된다() {
            ChatRoom room = fixture.createRoom("create-room", PRODUCT_ID, ChatRoomType.FUNDING, ChatRoomStatus.ACTIVE, CREATOR_ID);
            Long roomId = room.getId().value();

            BlackListCreateRequestDto request = new BlackListCreateRequestDto(TARGET_ID, "spam");
            BlackListReponseDto result = blackListService.create(roomId, CREATOR_ID, request);

            // Step 1: 응답 데이터 검증
            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.bannedUserId()).isEqualTo(TARGET_ID);
            assertThat(result.status()).isEqualTo("ACTIVE");

            // Step 2: DB 저장 검증
            Optional<FundingChatBlacklist> saved = blacklistRepository.findBlacklist(roomId, TARGET_ID);
            assertThat(saved).isPresent();
            assertThat(saved.get().getMemberId().value()).isEqualTo(TARGET_ID);
            assertThat(saved.get().getRoomId().value()).isEqualTo(roomId);
            assertThat(saved.get().getReason()).isEqualTo("spam");
        }
    }

    @Nested
    @DisplayName("블랙리스트 해제")
    class DeleteTest {

        // 해제 → 응답 DTO 검증 및 DB soft-delete(RELEASED) 상태 검증
        @Test
        @DisplayName("블랙리스트 해제 시 RELEASED 상태로 DB에 반영된다")
        void 성공적으로_블랙리스트를_해제한다() {
            ChatRoom room = fixture.createRoom("delete-room", PRODUCT_ID, ChatRoomType.FUNDING, ChatRoomStatus.ACTIVE, CREATOR_ID);
            Long roomId = room.getId().value();
            blackListService.create(roomId, CREATOR_ID, new BlackListCreateRequestDto(TARGET_ID, "setup"));

            BlackListReponseDto result = blackListService.delete(roomId, CREATOR_ID, TARGET_ID);

            // Step 1: 응답 데이터 검증
            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.bannedUserId()).isEqualTo(TARGET_ID);
            assertThat(result.status()).isEqualTo("RELEASED");

            // Step 2: DB soft-delete 검증 — ACTIVE 항목이 더 이상 존재하지 않음
            assertThat(blacklistRepository.isBlacklisted(roomId, TARGET_ID)).isFalse();
            assertThat(blacklistRepository.findBlacklist(roomId, TARGET_ID)).isEmpty();
        }

        // 해제 후 동일 대상 재추가 → 정합성(재차단 허용) 검증
        @Test
        @DisplayName("해제 후 동일 대상을 재추가할 수 있다")
        void 해제_후_동일_대상을_재추가할_수_있다() {
            ChatRoom room = fixture.createRoom("re-add-room", PRODUCT_ID, ChatRoomType.FUNDING, ChatRoomStatus.ACTIVE, CREATOR_ID);
            Long roomId = room.getId().value();

            blackListService.create(roomId, CREATOR_ID, new BlackListCreateRequestDto(TARGET_ID, "first"));
            blackListService.delete(roomId, CREATOR_ID, TARGET_ID);

            BlackListReponseDto result = blackListService.create(roomId, CREATOR_ID, new BlackListCreateRequestDto(TARGET_ID, "second"));

            // Step 1: 응답 검증
            assertThat(result.status()).isEqualTo("ACTIVE");

            // Step 2: DB에 ACTIVE 블랙리스트가 다시 존재
            assertThat(blacklistRepository.isBlacklisted(roomId, TARGET_ID)).isTrue();
        }
    }
}
