package com.bds.chat.infrastructure.messaging;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.common.events.funding.FundingStatusChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingCreatedListenerUnitTest {

    @Mock ChatRoomService chatRoomService;

    @InjectMocks FundingCreatedListener listener;

    private static final Long CREATOR_ID = 1L;
    private static final Long TARGET_ID  = 100L;

    private FundingStatusChangedEvent event(String type) {
        return FundingStatusChangedEvent.of(type, TARGET_ID, CREATOR_ID);
    }

    @Nested
    @DisplayName("이벤트 분기 처리")
    class DispatchTest {

        @Test
        @DisplayName("FUNDING_START 이벤트 수신 시 채팅방이 생성된다")
        void FUNDING_START_이벤트_수신_시_채팅방이_생성된다() {
            listener.handle(event("FUNDING_START"));

            verify(chatRoomService).createFundingRoom(eq(TARGET_ID), any(FundingRoomCreateRequestDto.class));
        }

        @Test
        @DisplayName("FUNDING_SUCCESS 이벤트 수신 시 채팅방이 삭제된다")
        void FUNDING_SUCCESS_이벤트_수신_시_채팅방이_삭제된다() {
            listener.handle(event("FUNDING_SUCCESS"));

            verify(chatRoomService).delete(TARGET_ID, CREATOR_ID);
        }

        @Test
        @DisplayName("FUNDING_FAIL 이벤트 수신 시 채팅방이 삭제된다")
        void FUNDING_FAIL_이벤트_수신_시_채팅방이_삭제된다() {
            listener.handle(event("FUNDING_FAIL"));

            verify(chatRoomService).delete(TARGET_ID, CREATOR_ID);
        }
    }

    @Nested
    @DisplayName("분기 경계")
    class BoundaryTest {

        @Test
        @DisplayName("FUNDING_SUCCESS와 FUNDING_FAIL은 동일한 분기에서 처리된다")
        void FUNDING_SUCCESS와_FUNDING_FAIL은_동일한_분기에서_처리된다() {
            listener.handle(event("FUNDING_SUCCESS"));
            listener.handle(event("FUNDING_FAIL"));

            verify(chatRoomService, times(2)).delete(TARGET_ID, CREATOR_ID);
            verify(chatRoomService, never()).createFundingRoom(any(), any());
        }

        @Test
        @DisplayName("FUNDING_START는 삭제를 호출하지 않는다")
        void FUNDING_START는_삭제를_호출하지_않는다() {
            listener.handle(event("FUNDING_START"));

            verify(chatRoomService, never()).delete(any(), any());
        }

        @Test
        @DisplayName("FUNDING_START 요청 시 creatorId가 FundingRoomCreateRequestDto에 담겨 전달된다")
        void FUNDING_START_요청_시_creatorId가_FundingRoomCreateRequestDto에_담겨_전달된다() {
            listener.handle(event("FUNDING_START"));

            verify(chatRoomService).createFundingRoom(eq(TARGET_ID), eq(new FundingRoomCreateRequestDto(CREATOR_ID)));
        }

        @Test
        @DisplayName("알 수 없는 type의 이벤트는 아무 서비스도 호출하지 않는다")
        void 알_수_없는_type의_이벤트는_아무_서비스도_호출하지_않는다() {
            listener.handle(event("UNKNOWN_TYPE"));

            verifyNoInteractions(chatRoomService);
        }
    }
}
