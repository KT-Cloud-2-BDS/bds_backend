package com.bds.payment.payment.application.payment;

import com.bds.payment.payment.application.accounts.AccountService;
import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.common.PaymentHistoryStatus;
import com.bds.payment.payment.domain.common.TransactionReason;
import com.bds.payment.payment.domain.common.TransactionType;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.infrastructure.external.BankClient;
import com.bds.payment.payment.infrastructure.external.request.BankTransactionRequestDto;
import com.bds.payment.payment.presentation.request.AccountTransactionRequestDto;
import com.bds.payment.payment.presentation.response.AccountTransactionResponseDto;
import com.bds.payment.payment.presentation.response.PaymentHistoryPageResponseDto;
import com.bds.payment.payment.presentation.response.PaymentHistoryResponseDto;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final AccountService accountService;
    private final WalletService walletService;
    private final BankClient bankClient;

    public AccountTransactionResponseDto charge(Long memberId,AccountTransactionRequestDto dto) {
        Account account = accountService.getAccount(memberId);
        UUID tranSeqNo = createUuid();

        Wallet updatedWallet = walletService.charge(memberId, dto.amount());

        BankTransactionRequestDto requestDto = BankTransactionRequestDto.create(account.getAccountNumber(), dto.amount(), tranSeqNo);
        bankClient.withdraw(requestDto);

        PaymentHistoryCommand command = PaymentHistoryCommand.of(
                updatedWallet,
                tranSeqNo,
                TransactionType.DEPOSIT,
                TransactionReason.CHARGE,
                dto.amount(),
                PaymentHistoryStatus.SUCCESS);

        paymentHistoryRepository.save(PaymentHistory.create(command));

        return new AccountTransactionResponseDto(updatedWallet.getBalance(), tranSeqNo);
    }

    public AccountTransactionResponseDto withdraw(Long memberId,AccountTransactionRequestDto dto) {
        Account account = accountService.getAccount(memberId);
        UUID tranSeqNo = createUuid();

        Wallet updatedWallet = walletService.decrease(memberId, dto.amount());

        PaymentHistoryCommand command = PaymentHistoryCommand.of(
                updatedWallet,
                tranSeqNo,
                TransactionType.WITHDRAWAL,
                TransactionReason.WITHDRAW,
                dto.amount(),
                PaymentHistoryStatus.SUCCESS);
        paymentHistoryRepository.save(PaymentHistory.create(command));

        BankTransactionRequestDto requestDto = BankTransactionRequestDto.create(account.getAccountNumber(), dto.amount(), tranSeqNo);

        bankClient.deposit(requestDto);

        return new AccountTransactionResponseDto(updatedWallet.getBalance(), tranSeqNo);
    }

    @Transactional(readOnly = true)
    public PaymentHistoryPageResponseDto getHistory(Long memberId, LocalDate from, LocalDate to, Pageable pageable) {
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusMonths(1);
        if (from.isAfter(to)) throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);

        Long walletId = walletService.getWalletId(memberId);

        Page<PaymentHistory> result = paymentHistoryRepository.findByWalletIdAndCreatedAtBetween(
                walletId,
                from.atStartOfDay(),
                to.atTime(LocalTime.MAX),
                pageable
        );

        return PaymentHistoryPageResponseDto.from(result.map(PaymentHistoryResponseDto::from));
    }

    private UUID createUuid() {
        return UuidCreator.getTimeOrderedEpoch();
    }

}
