package com.bds.payment.payment.application.payment;

import com.bds.payment.payment.application.accounts.AccountService;
import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.infrastructure.external.BankClient;
import com.bds.payment.payment.infrastructure.external.request.BankTransactionRequestDto;
import com.bds.payment.payment.presentation.request.AccountTransactionRequestDto;
import com.bds.payment.payment.presentation.response.PaymentHistoryPageResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private AccountService accountService;
    @Mock private WalletService walletService;
    @Mock private BankClient bankClient;

    @InjectMocks private PaymentService paymentService;

    @Nested
    @DisplayName("페이 충전 정상 테스트")
    class chargeTest {

        @Test
        public void 페이를_정상적으로_충전한다() {
            // given
            Long memberId = 1L;
            AccountTransactionRequestDto dto = new AccountTransactionRequestDto(10000L);
            Account account = Account.builder()
                    .walletId(1L)
                    .accountNumber("1234567890")
                    .build();
            Wallet updatedWallet = Wallet.builder()
                    .id(1L)
                    .memberId(memberId)
                    .balance(10000L)
                    .build();
            given(accountService.getAccount(memberId)).willReturn(account);
            given(walletService.charge(memberId, dto.amount())).willReturn(updatedWallet);
            given(paymentHistoryRepository.save(any(PaymentHistory.class))).willReturn(PaymentHistory.builder().build());

            // when
            paymentService.charge(memberId, dto);

            // then
            verify(bankClient).withdraw(any(BankTransactionRequestDto.class));
            verify(walletService).charge(memberId, dto.amount());
            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
        }
    }

    @Nested
    @DisplayName("페이 출금 정상 테스트")
    class withdrawTest {

        @Test
        public void 페이를_정상적으로_출금한다() {
            // given
            Long memberId = 1L;
            AccountTransactionRequestDto dto = new AccountTransactionRequestDto(10000L);
            Account account = Account.builder()
                    .walletId(1L)
                    .accountNumber("1234567890")
                    .build();
            Wallet updatedWallet = Wallet.builder()
                    .id(1L)
                    .memberId(memberId)
                    .balance(40000L)
                    .build();
            given(accountService.getAccount(memberId)).willReturn(account);
            given(walletService.decrease(memberId, dto.amount())).willReturn(updatedWallet);
            given(paymentHistoryRepository.save(any(PaymentHistory.class))).willReturn(PaymentHistory.builder().build());

            // when
            paymentService.withdraw(memberId, dto);

            // then
            verify(walletService).decrease(memberId, dto.amount());
            verify(bankClient).deposit(any(BankTransactionRequestDto.class));
            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
        }
    }

    @Nested
    @DisplayName("거래 내역 조회 정상 테스트")
    class getHistoryTest {

        @Test
        public void 거래_내역을_정상적으로_조회한다() {
            // given
            Long memberId = 1L;
            LocalDate from = LocalDate.now().minusMonths(1);
            LocalDate to = LocalDate.now();
            Pageable pageable = PageRequest.of(0, 20);
            Long walletId = 1L;
            Page<PaymentHistory> page = Page.empty();
            given(walletService.getWalletId(memberId)).willReturn(walletId);
            given(paymentHistoryRepository.findByWalletIdAndCreatedAtBetween(
                    eq(walletId),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    eq(pageable)
            )).willReturn(page);

            // when
            PaymentHistoryPageResponseDto result = paymentService.getHistory(memberId, from, to, pageable);

            // then
            assertNotNull(result);
            verify(paymentHistoryRepository).findByWalletIdAndCreatedAtBetween(
                    eq(walletId),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    eq(pageable)
            );
        }

        @Test
        public void from_to_null이면_기본값으로_조회한다() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            Long walletId = 1L;
            Page<PaymentHistory> page = Page.empty();
            given(walletService.getWalletId(memberId)).willReturn(walletId);
            given(paymentHistoryRepository.findByWalletIdAndCreatedAtBetween(
                    eq(walletId),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    eq(pageable)
            )).willReturn(page);

            // when
            PaymentHistoryPageResponseDto result = paymentService.getHistory(memberId, null, null, pageable);

            // then
            assertNotNull(result);
            verify(paymentHistoryRepository).findByWalletIdAndCreatedAtBetween(
                    eq(walletId),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    eq(pageable)
            );
        }
    }
}