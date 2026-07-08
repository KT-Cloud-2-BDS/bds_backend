package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.response.FundingPaymentResponseDto;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FundingService {

    private final FundingPaymentRepository fundingPaymentRepository;
    private final WalletService walletService;

    public FundingPaymentResponseDto funding(FundingPaymentRequestDto dto) {
        validateDuplicated(dto.orderId());

        UUID tranSeqNo = UuidCreator.getTimeOrderedEpoch();

        Long fromWalletId = walletService.getWalletId(dto.memberId());

        if (dto.paymentType() == PaymentType.INSTANT) {
            walletService.decrease(dto.memberId(), dto.amount());
        }

        FundingPayment savedFunding = fundingPaymentRepository.save(FundingPayment.create(dto, fromWalletId, tranSeqNo));

        return FundingPaymentResponseDto.from(savedFunding, dto.memberId());
    }

    public void refund(Long memberId, Long orderId) {
        FundingPayment fundingPayment = fundingPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래입니다."));

        if (fundingPayment.getStatus() == FundingPaymentStatus.REFUNDED)
            throw new IllegalArgumentException("이미 환불된 거래입니다.");

        if (fundingPayment.getPaymentType() == PaymentType.INSTANT) {
            walletService.charge(memberId, fundingPayment.getAmount());
        }

        fundingPayment.refund();

        fundingPaymentRepository.save(fundingPayment);
    }

    private void validateDuplicated(Long orderId) {
        if (fundingPaymentRepository.existsByOrderId(orderId))
            throw new IllegalArgumentException("중복된 거래입니다.");
    }
}
