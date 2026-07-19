package com.bds.payment.payment.application.funding.consumer;

import com.bds.common.events.payment.RefundRequestEvent;
import com.bds.messaging.idempotency.ProcessedEventStore;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundRequestListenerUnitTest {
    @Mock private FundingService fundingService;
    @Mock private ProcessedEventStore processedEventStore;

    @InjectMocks private RefundRequestListener listener;

    private RefundRequestEvent createEvent() {
        return new RefundRequestEvent(
                UuidCreator.getTimeOrderedEpoch(),
                101L,
                1L,
                10000L,
                "USER_CANCEL"
        );
    }

    @Test
    void 이벤트를_수신하면_FundingService_refund를_호출한다() {
        // given
        RefundRequestEvent event = createEvent();
        given(processedEventStore.markProcessed(event.requestId())).willReturn(true);

        // when
        listener.handle(event);

        // then
        verify(fundingService).refund(any(RefundRequestDto.class));
    }

    @Test
    void 중복_이벤트는_FundingService_호출없이_스킵한다() {
        // given
        RefundRequestEvent event = createEvent();
        given(processedEventStore.markProcessed(event.requestId())).willReturn(false);

        // when
        listener.handle(event);

        // then
        verify(fundingService, never()).refund(any());
    }

    @Test
    void FUNDING_ALREADY_REFUNDED_예외는_스킵한다() {
        // given
        RefundRequestEvent event = createEvent();
        given(processedEventStore.markProcessed(event.requestId())).willReturn(true);
        doThrow(new BusinessException(ErrorCode.FUNDING_ALREADY_REFUNDED))
                .when(fundingService).refund(any());

        // when & then
        assertDoesNotThrow(() -> listener.handle(event));
    }

    @Test
    void 다른_BusinessException은_전파한다() {
        // given
        RefundRequestEvent event = createEvent();
        given(processedEventStore.markProcessed(event.requestId())).willReturn(true);
        doThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND))
                .when(fundingService).refund(any());

        // when & then
        assertThrows(BusinessException.class, () -> listener.handle(event));
    }

    @Test
    void RuntimeException은_전파한다() {
        // given
        RefundRequestEvent event = createEvent();
        given(processedEventStore.markProcessed(event.requestId())).willReturn(true);
        doThrow(new RuntimeException("DB 오류"))
                .when(fundingService).refund(any());

        // when & then
        assertThrows(RuntimeException.class, () -> listener.handle(event));
    }

    @Test
    void 이벤트_필드가_DTO에_정확히_전달된다() {
        // given
        UUID requestId = UuidCreator.getTimeOrderedEpoch();
        RefundRequestEvent event = new RefundRequestEvent(
                requestId,
                101L,
                1L,
                10000L,
                "USER_CANCEL"
        );
        given(processedEventStore.markProcessed(event.requestId())).willReturn(true);

        // when
        listener.handle(event);

        // then
        verify(fundingService).refund(argThat(dto ->
                dto.requestId().equals(requestId) &&
                        dto.orderId().equals(101L) &&
                        dto.memberId().equals(1L) &&
                        dto.amount().equals(10000L) &&
                        dto.cancelReason().equals("USER_CANCEL")
        ));
    }
}