package com.bds.chat.infrastructure.messaging;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.common.events.funding.FundingCreatedEvent;
import com.bds.common.events.funding.FundingType;
import com.bds.messaging.idempotency.ProcessedEventStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingCreatedListenerUnitTest {

    @Mock ProcessedEventStore processedEventStore;
    @Mock ChatRoomService chatRoomService;

    @InjectMocks FundingCreatedListener listener;

    private static final Long CREATOR_ID = 1L;
    private static final Long TARGET_ID = 100L;

    private FundingCreatedEvent event(FundingType type) {
        return new FundingCreatedEvent(UUID.randomUUID(), type, "PRODUCT", TARGET_ID, CREATOR_ID);
    }

    @Nested
    @DisplayName("이벤트 분기 처리")
    class DispatchTest {

        @Test
        void FUNDING_START_이벤트_수신_시_채팅방이_생성된다() {
            given(processedEventStore.markProcessed(any())).willReturn(true);

            listener.handle(event(FundingType.FUNDING_START));

            verify(chatRoomService).createFundingRoom(eq(TARGET_ID), any(FundingRoomCreateRequestDto.class));
        }

        @Test
        void FUNDING_SUCCESS_이벤트_수신_시_채팅방이_삭제된다() {
            given(processedEventStore.markProcessed(any())).willReturn(true);

            listener.handle(event(FundingType.FUNDING_SUCCESS));

            verify(chatRoomService).delete(TARGET_ID, CREATOR_ID);
        }

        @Test
        void FUNDING_FAIL_이벤트_수신_시_채팅방이_삭제된다() {
            given(processedEventStore.markProcessed(any())).willReturn(true);

            listener.handle(event(FundingType.FUNDING_FAIL));

            verify(chatRoomService).delete(TARGET_ID, CREATOR_ID);
        }

        @Test
        void 중복_eventId_이벤트는_스킵된다() {
            given(processedEventStore.markProcessed(any())).willReturn(false);

            listener.handle(event(FundingType.FUNDING_START));

            verifyNoInteractions(chatRoomService);
        }
    }

    @Nested
    @DisplayName("임계점: switch case 경계")
    class BoundaryTest {

        @Test
        void FUNDING_SUCCESS와_FUNDING_FAIL은_동일한_분기에서_처리된다() {
            given(processedEventStore.markProcessed(any())).willReturn(true);

            listener.handle(event(FundingType.FUNDING_SUCCESS));
            listener.handle(event(FundingType.FUNDING_FAIL));

            verify(chatRoomService, times(2)).delete(TARGET_ID, CREATOR_ID);
            verify(chatRoomService, never()).createFundingRoom(any(), any());
        }

        @Test
        void FUNDING_START는_삭제를_호출하지_않는다() {
            given(processedEventStore.markProcessed(any())).willReturn(true);

            listener.handle(event(FundingType.FUNDING_START));

            verify(chatRoomService, never()).delete(any(), any());
        }

        @Test
        void FUNDING_START_요청_시_creatorId가_FundingRoomCreateRequestDto에_담겨_전달된다() {
            given(processedEventStore.markProcessed(any())).willReturn(true);

            listener.handle(event(FundingType.FUNDING_START));

            verify(chatRoomService).createFundingRoom(eq(TARGET_ID), eq(new FundingRoomCreateRequestDto(CREATOR_ID)));
        }
    }
}
