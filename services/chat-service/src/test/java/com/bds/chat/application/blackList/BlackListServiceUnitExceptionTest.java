package com.bds.chat.application.blackList;

import com.bds.chat.application.blackList.dto.BlackListCreateRequestDto;
import com.bds.chat.application.blackList.service.BlackListService;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.blackList.BlacklistStatus;
import com.bds.chat.domain.blackList.FundingChatBlacklist;
import com.bds.chat.domain.blackList.FundingChatBlacklistRepository;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BlackListServiceUnitExceptionTest {

    @Mock FundingChatBlacklistRepository fundingChatBlacklistRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock Clock clock;

    @InjectMocks BlackListService blackListService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final Long ROOM_ID = 10L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Long TARGET_ID = 7L;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private ChatRoom fundingRoom() {
        return ChatRoom.restore(ChatRoomId.of(ROOM_ID), MemberId.of(CREATOR_ID), ProductId.of(PRODUCT_ID),
                null, ChatRoomStatus.ACTIVE, ChatRoomType.FUNDING, NOW, null);
    }

    private ChatRoom inquiryRoom() {
        return ChatRoom.restore(ChatRoomId.of(ROOM_ID), MemberId.of(CREATOR_ID), ProductId.of(PRODUCT_ID),
                null, ChatRoomStatus.ACTIVE, ChatRoomType.INQUIRY, NOW, null);
    }

    private FundingChatBlacklist activeBlacklist() {
        return FundingChatBlacklist.restore(FundingChatBlacklistId.of(1L),
                ChatRoomId.of(ROOM_ID), MemberId.of(TARGET_ID), null, BlacklistStatus.ACTIVE, NOW, null);
    }

    @Nested
    @DisplayName("블랙리스트 추가 예외")
    class CreateExceptionTest {

        @Test
        void 자기_자신을_차단하면_INVALID_INPUT_예외() {
            BlackListCreateRequestDto request = new BlackListCreateRequestDto(CREATOR_ID, null);

            assertThatThrownBy(() -> blackListService.create(ROOM_ID, CREATOR_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        void 방이_없으면_NOT_FOUND_예외() {
            BlackListCreateRequestDto request = new BlackListCreateRequestDto(TARGET_ID, null);

            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> blackListService.create(ROOM_ID, CREATOR_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void INQUIRY_방이면_FORBIDDEN_예외() {
            BlackListCreateRequestDto request = new BlackListCreateRequestDto(TARGET_ID, null);

            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.of(inquiryRoom()));

            assertThatThrownBy(() -> blackListService.create(ROOM_ID, CREATOR_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        void creator가_아니면_FORBIDDEN_예외() {
            Long nonCreatorId = 99L;
            BlackListCreateRequestDto request = new BlackListCreateRequestDto(TARGET_ID, null);

            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.of(fundingRoom()));

            assertThatThrownBy(() -> blackListService.create(ROOM_ID, nonCreatorId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        void 이미_차단된_유저면_CONFLICT_예외() {
            BlackListCreateRequestDto request = new BlackListCreateRequestDto(TARGET_ID, null);

            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.of(fundingRoom()));
            lenient().when(fundingChatBlacklistRepository.isBlacklisted(ROOM_ID, TARGET_ID)).thenReturn(true);

            assertThatThrownBy(() -> blackListService.create(ROOM_ID, CREATOR_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }

    @Nested
    @DisplayName("블랙리스트 해제 예외")
    class DeleteExceptionTest {

        @Test
        void 방이_없으면_NOT_FOUND_예외() {
            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> blackListService.delete(ROOM_ID, CREATOR_ID, TARGET_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        void INQUIRY_방이면_FORBIDDEN_예외() {
            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.of(inquiryRoom()));

            assertThatThrownBy(() -> blackListService.delete(ROOM_ID, CREATOR_ID, TARGET_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        void creator가_아니면_FORBIDDEN_예외() {
            Long nonCreatorId = 99L;

            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.of(fundingRoom()));

            assertThatThrownBy(() -> blackListService.delete(ROOM_ID, nonCreatorId, TARGET_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        void 활성_차단이_없으면_NOT_FOUND_예외() {
            lenient().when(chatRoomRepository.findActiveById(ROOM_ID)).thenReturn(Optional.of(fundingRoom()));
            lenient().when(fundingChatBlacklistRepository.findBlacklist(ROOM_ID, TARGET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> blackListService.delete(ROOM_ID, CREATOR_ID, TARGET_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }
}
