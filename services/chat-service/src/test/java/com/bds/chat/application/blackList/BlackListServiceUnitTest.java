package com.bds.chat.application.blackList;

import com.bds.chat.application.blackList.dto.BlackListCreateRequestDto;
import com.bds.chat.application.blackList.dto.BlackListReponseDto;
import com.bds.chat.application.blackList.service.BlackListService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlackListServiceUnitTest {

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
        given(clock.instant()).willReturn(Instant.parse("2026-01-01T00:00:00Z"));
        given(clock.getZone()).willReturn(ZoneOffset.UTC);
    }

    private ChatRoom fundingRoom() {
        return ChatRoom.restore(ChatRoomId.of(ROOM_ID), MemberId.of(CREATOR_ID), ProductId.of(PRODUCT_ID),
                null, ChatRoomStatus.ACTIVE, ChatRoomType.FUNDING, NOW, null);
    }

    private FundingChatBlacklist activeBlacklist() {
        return FundingChatBlacklist.restore(FundingChatBlacklistId.of(1L),
                ChatRoomId.of(ROOM_ID), MemberId.of(TARGET_ID), null, BlacklistStatus.ACTIVE, NOW, null);
    }

    private FundingChatBlacklist releasedBlacklist() {
        return FundingChatBlacklist.restore(FundingChatBlacklistId.of(1L),
                ChatRoomId.of(ROOM_ID), MemberId.of(TARGET_ID), null, BlacklistStatus.RELEASED, NOW, NOW);
    }

    @Nested
    @DisplayName("블랙리스트 추가")
    class CreateTest {

        @Test
        void 성공적으로_블랙리스트를_추가한다() {
            BlackListCreateRequestDto request = new BlackListCreateRequestDto(TARGET_ID, "spam");

            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(fundingChatBlacklistRepository.isBlacklisted(ROOM_ID, TARGET_ID)).willReturn(false);
            given(fundingChatBlacklistRepository.save(any())).willReturn(activeBlacklist());

            BlackListReponseDto result = blackListService.create(ROOM_ID, CREATOR_ID, request);

            assertThat(result.roomId()).isEqualTo(ROOM_ID);
            assertThat(result.bannedUserId()).isEqualTo(TARGET_ID);
            assertThat(result.status()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("블랙리스트 해제")
    class DeleteTest {

        @Test
        void 성공적으로_블랙리스트를_해제한다() {
            given(chatRoomRepository.findActiveById(ROOM_ID)).willReturn(Optional.of(fundingRoom()));
            given(fundingChatBlacklistRepository.findBlacklist(ROOM_ID, TARGET_ID))
                    .willReturn(Optional.of(activeBlacklist()));
            given(fundingChatBlacklistRepository.save(any())).willReturn(releasedBlacklist());

            BlackListReponseDto result = blackListService.delete(ROOM_ID, CREATOR_ID, TARGET_ID);

            assertThat(result.roomId()).isEqualTo(ROOM_ID);
            assertThat(result.bannedUserId()).isEqualTo(TARGET_ID);
            assertThat(result.status()).isEqualTo("RELEASED");
        }
    }
}
