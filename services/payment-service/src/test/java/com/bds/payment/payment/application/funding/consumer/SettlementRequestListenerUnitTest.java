package com.bds.payment.payment.application.funding.consumer;

import com.bds.payment.payment.presentation.request.SettlementRequestEvent;
import com.bds.messaging.idempotency.ProcessedEventStore;
import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.domain.common.SettlementType;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SettlementRequestListenerUnitTest {
    @Mock private FundingService fundingService;
    @Mock private ProcessedEventStore processedEventStore;

    @InjectMocks private SettlementRequestListener listener;

    private SettlementRequestEvent createEvent(String type) {
        return new SettlementRequestEvent(
                UuidCreator.getTimeOrderedEpoch(),
                type,
                999L,
                100L,
                List.of(
                        new SettlementRequestEvent.Item(101L, 1L, 10000L),
                        new SettlementRequestEvent.Item(102L, 2L, 20000L)
                )
        );
    }

    @Nested
    @DisplayName("타입별 서비스 호출")
    class TypeRoutingTest {

        @Test
        void SETTLEMENT_CONFIRMED_타입은_confirmSettlement를_호출한다() {
            // given
            SettlementRequestEvent event = createEvent("SETTLEMENT_CONFIRMED");
            given(processedEventStore.markProcessed(event.batchId())).willReturn(true);

            // when
            listener.handle(event);

            // then
            verify(fundingService).confirmSettlement(any(SettlementBatchRequestDto.class));
            verify(fundingService, never()).confirmReservedFunding(any());
            verify(fundingService, never()).refundFailedFunding(any());
        }

        @Test
        void RESERVED_FUNDING_CONFIRMED_타입은_confirmReservedFunding을_호출한다() {
            // given
            SettlementRequestEvent event = createEvent("RESERVED_FUNDING_CONFIRMED");
            given(processedEventStore.markProcessed(event.batchId())).willReturn(true);

            // when
            listener.handle(event);

            // then
            verify(fundingService).confirmReservedFunding(any(SettlementBatchRequestDto.class));
            verify(fundingService, never()).confirmSettlement(any());
            verify(fundingService, never()).refundFailedFunding(any());
        }

        @Test
        void FUNDING_FAILED_REFUND_타입은_refundFailedFunding을_호출한다() {
            // given
            SettlementRequestEvent event = createEvent("FUNDING_FAILED_REFUND");
            given(processedEventStore.markProcessed(event.batchId())).willReturn(true);

            // when
            listener.handle(event);

            // then
            verify(fundingService).refundFailedFunding(any(SettlementBatchRequestDto.class));
            verify(fundingService, never()).confirmSettlement(any());
            verify(fundingService, never()).confirmReservedFunding(any());
        }

        @Test
        void 알수없는_타입은_IllegalArgumentException을_던진다() {
            // given
            SettlementRequestEvent event = createEvent("UNKNOWN_TYPE");
            given(processedEventStore.markProcessed(event.batchId())).willReturn(true);

            // when & then
            assertThrows(IllegalArgumentException.class, () -> listener.handle(event));
            verify(fundingService, never()).confirmSettlement(any());
            verify(fundingService, never()).confirmReservedFunding(any());
            verify(fundingService, never()).refundFailedFunding(any());
        }
    }

    @Nested
    @DisplayName("멱등성 처리")
    class IdempotencyTest {

        @Test
        void 중복_배치는_스킵한다() {
            // given
            SettlementRequestEvent event = createEvent("SETTLEMENT_CONFIRMED");
            given(processedEventStore.markProcessed(event.batchId())).willReturn(false);

            // when
            listener.handle(event);

            // then
            verifyNoInteractions(fundingService);
        }
    }

    @Nested
    @DisplayName("DTO 매핑")
    class DtoMappingTest {

        @Test
        void 이벤트_필드가_DTO에_정확히_전달된다() {
            // given
            UUID batchId = UuidCreator.getTimeOrderedEpoch();
            SettlementRequestEvent event = new SettlementRequestEvent(
                    batchId,
                    "SETTLEMENT_CONFIRMED",
                    999L,
                    100L,
                    List.of(
                            new SettlementRequestEvent.Item(101L, 1L, 10000L),
                            new SettlementRequestEvent.Item(102L, 2L, 20000L)
                    )
            );
            given(processedEventStore.markProcessed(event.batchId())).willReturn(true);

            // when
            listener.handle(event);

            // then
            verify(fundingService).confirmSettlement(argThat(dto ->
                    dto.batchId().equals(batchId) &&
                            dto.type() == SettlementType.SETTLEMENT_CONFIRMED &&
                            dto.creatorMemberId().equals(999L) &&
                            dto.productId().equals(100L) &&
                            dto.items().size() == 2 &&
                            dto.items().getFirst().orderId().equals(101L) &&
                            dto.items().getFirst().memberId().equals(1L) &&
                            dto.items().getFirst().amount().equals(10000L)
            ));
        }
    }
}