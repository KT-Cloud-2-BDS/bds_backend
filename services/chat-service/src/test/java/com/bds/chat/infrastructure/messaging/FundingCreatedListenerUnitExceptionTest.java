package com.bds.chat.infrastructure.messaging;

import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.common.events.funding.FundingStatusChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class FundingCreatedListenerUnitExceptionTest {

    @Mock ChatRoomService chatRoomService;

    @InjectMocks FundingCreatedListener listener;

    private static final Long CREATOR_ID = 1L;
    private static final Long TARGET_ID  = 100L;

    private FundingStatusChangedEvent event(String type) {
        return FundingStatusChangedEvent.of(type, TARGET_ID, CREATOR_ID);
    }

    @Nested
    @DisplayName("FUNDING_START 예외 처리")
    class FundingStartExceptionTest {

        @Test
        @DisplayName("BusinessException 발생 시 예외를 삼켜 메시지가 ACK된다")
        void BusinessException_발생시_예외를_삼켜_메시지가_ack된다() {
            doThrow(new BusinessException(ErrorCode.CONFLICT))
                    .when(chatRoomService).createFundingRoom(any(), any());

            assertThatCode(() -> listener.handle(event("FUNDING_START")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("일반 Exception 발생 시 재전파하여 RabbitMQ 재시도를 트리거한다")
        void 일반_Exception_발생시_재전파하여_RabbitMQ_재시도를_트리거한다() {
            doThrow(new RuntimeException("DB 연결 실패"))
                    .when(chatRoomService).createFundingRoom(any(), any());

            assertThatThrownBy(() -> listener.handle(event("FUNDING_START")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 연결 실패");
        }
    }

    @Nested
    @DisplayName("FUNDING_SUCCESS / FUNDING_FAIL 예외 처리")
    class FundingEndExceptionTest {

        @Test
        @DisplayName("FUNDING_SUCCESS에서 BusinessException 발생 시 예외를 삼켜 메시지가 ACK된다")
        void FUNDING_SUCCESS에서_BusinessException_발생시_예외를_삼켜_메시지가_ack된다() {
            doThrow(new BusinessException(ErrorCode.NOT_FOUND))
                    .when(chatRoomService).delete(any(), any());

            assertThatCode(() -> listener.handle(event("FUNDING_SUCCESS")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("FUNDING_FAIL에서 BusinessException 발생 시 예외를 삼켜 메시지가 ACK된다")
        void FUNDING_FAIL에서_BusinessException_발생시_예외를_삼켜_메시지가_ack된다() {
            doThrow(new BusinessException(ErrorCode.NOT_FOUND))
                    .when(chatRoomService).delete(any(), any());

            assertThatCode(() -> listener.handle(event("FUNDING_FAIL")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("FUNDING_SUCCESS에서 일반 Exception 발생 시 재전파하여 RabbitMQ 재시도를 트리거한다")
        void FUNDING_SUCCESS에서_일반_Exception_발생시_재전파하여_RabbitMQ_재시도를_트리거한다() {
            doThrow(new RuntimeException("DB 연결 실패"))
                    .when(chatRoomService).delete(any(), any());

            assertThatThrownBy(() -> listener.handle(event("FUNDING_SUCCESS")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 연결 실패");
        }

        @Test
        @DisplayName("FUNDING_FAIL에서 일반 Exception 발생 시 재전파하여 RabbitMQ 재시도를 트리거한다")
        void FUNDING_FAIL에서_일반_Exception_발생시_재전파하여_RabbitMQ_재시도를_트리거한다() {
            doThrow(new RuntimeException("DB 연결 실패"))
                    .when(chatRoomService).delete(any(), any());

            assertThatThrownBy(() -> listener.handle(event("FUNDING_FAIL")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 연결 실패");
        }
    }
}
