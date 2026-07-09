package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FundingServiceIntegrationExceptionTest {

    @Autowired
    private FundingService fundingService;

    @Autowired
    private FundingPaymentRepository fundingPaymentRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void 중복된_주문이면_예외를_던진다() {
        FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
        Wallet wallet = walletRepository.save(Wallet.builder().memberId(dto.memberId()).balance(0L).build());
        FundingPayment saved = FundingPayment.create(dto, wallet.getId(), UuidCreator.getTimeOrderedEpoch());
        fundingPaymentRepository.save(saved);

        assertThatThrownBy(() -> fundingService.funding(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 존재하지_않는_거래를_환불하면_예외를_던진다() {
        Long memberId = 1L;
        Long orderId = 1L;

        assertThatThrownBy(() -> fundingService.refund(memberId, orderId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이미_환불된_거래를_환불하면_예외를_던진다() {
        Long memberId = 1L;
        Long orderId = 1L;
        Wallet wallet = walletRepository.save(Wallet.builder().memberId(memberId).balance(10000L).build());
        FundingPayment fundingPayment = FundingPayment.builder()
                .orderId(orderId)
                .walletId(wallet.getId())
                .productId(100L)
                .tranSeqNo(java.util.UUID.randomUUID())
                .amount(10000L)
                .paymentType(PaymentType.INSTANT)
                .status(FundingPaymentStatus.REFUNDED)
                .build();
        fundingPaymentRepository.save(fundingPayment);

        assertThatThrownBy(() -> fundingService.refund(memberId, orderId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
