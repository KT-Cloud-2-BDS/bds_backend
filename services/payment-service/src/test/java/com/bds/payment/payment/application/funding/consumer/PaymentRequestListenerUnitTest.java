package com.bds.payment.payment.application.funding.consumer;

import com.bds.payment.payment.presentation.request.PaymentRequestEvent;
import com.bds.messaging.idempotency.ProcessedEventStore;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRequestListenerUnitTest {
    @Mock private FundingService fundingService;
    @Mock private ProcessedEventStore processedEventStore;

    @InjectMocks private PaymentRequestListener listener;

    @Test
    void 이벤트를_수신하면_FundingService_funding을_호출한다() {
        // given
        PaymentRequestEvent event = new PaymentRequestEvent(
                UUID.randomUUID(), 101L, 1L, 100L, 10000L, "INSTANT"
        );
        given(processedEventStore.markProcessed(event.requestId())).willReturn(true);

        // when
        listener.handle(event);

        // then
        verify(fundingService).funding(any(FundingPaymentRequestDto.class));
    }

    @Test
    void 중복_이벤트는_스킵한다() {
        // given
        PaymentRequestEvent event = new PaymentRequestEvent(
                UUID.randomUUID(), 101L, 1L, 100L, 10000L, "INSTANT"
        );
        given(processedEventStore.markProcessed(event.requestId())).willReturn(false);

        // when
        listener.handle(event);

        // then
        verify(fundingService, never()).funding(any());
    }

    @Test
    void FUNDING_DUPLICATED_예외는_스킵한다() {
        // given
        PaymentRequestEvent event = new PaymentRequestEvent(
                UUID.randomUUID(), 101L, 1L, 100L, 10000L, "INSTANT"
        );
        given(processedEventStore.markProcessed(event.requestId())).willReturn(true);
        doThrow(new BusinessException(ErrorCode.FUNDING_DUPLICATED))
                .when(fundingService).funding(any());

        // when & then
        assertDoesNotThrow(() -> listener.handle(event));
    }

    @Test
    void 다른_BusinessException은_전파한다() {
        // given
        PaymentRequestEvent event = new PaymentRequestEvent(
                UUID.randomUUID(), 101L, 1L, 100L, 10000L, "INSTANT"
        );
        given(processedEventStore.markProcessed(event.requestId())).willReturn(true);
        doThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND))
                .when(fundingService).funding(any());

        // when & then
        assertThrows(BusinessException.class, () -> listener.handle(event));
    }
}