package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.response.FundingPaymentResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FundingServiceIntegrationTest {

    @Autowired
    private FundingService fundingService;

    @Autowired
    private FundingPaymentRepository fundingPaymentRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void INSTANT_결제를_정상_처리한다() {
        FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
        Wallet wallet = Wallet.builder().memberId(dto.memberId()).balance(40000L).build();
        walletRepository.save(wallet);

        FundingPaymentResponseDto result = fundingService.funding(dto);

        assertThat(result).isNotNull();
        assertThat(fundingPaymentRepository.findByOrderId(dto.orderId())).isPresent();
    }

    @Test
    void RESERVED_결제를_정상_처리한다() {
        Long memberId = 1L;
        FundingPaymentRequestDto dto = new FundingPaymentRequestDto(2L, memberId, 100L, 10000L, PaymentType.RESERVED);
        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .balance(0L)
                .build();
        walletRepository.save(wallet);
        walletService.getWalletId(dto.memberId());

        FundingPaymentResponseDto result = fundingService.funding(dto);

        assertThat(result).isNotNull();
        assertThat(fundingPaymentRepository.findByOrderId(dto.orderId())).isPresent();
    }
}
