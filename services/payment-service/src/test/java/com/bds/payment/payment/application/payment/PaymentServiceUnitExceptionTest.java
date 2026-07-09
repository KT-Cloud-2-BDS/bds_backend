package com.bds.payment.payment.application.payment;

import com.bds.payment.payment.application.accounts.AccountService;
import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.infrastructure.external.BankClient;
import com.bds.payment.payment.infrastructure.external.request.BankTransactionRequestDto;
import com.bds.payment.payment.presentation.request.AccountTransactionRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitExceptionTest {

    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private AccountService accountService;
    @Mock private WalletService walletService;
    @Mock private BankClient bankClient;

    @InjectMocks private PaymentService paymentService;

    @Nested
    @DisplayName("페이 충전 예외 테스트")
    class chargeExceptionTest {

        @Test
        public void 계좌가_없으면_예외를_던진다() {
            // given
            Long memberId = 1L;
            AccountTransactionRequestDto dto = new AccountTransactionRequestDto(10000L);
            given(accountService.getAccount(memberId)).willThrow(new IllegalArgumentException("잘못된 접근입니다."));

            // when & then
            assertThatThrownBy(() -> paymentService.charge(memberId, dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(bankClient, never()).withdraw(any(BankTransactionRequestDto.class));
            verify(walletService, never()).charge(any(), any());
        }
    }

    @Nested
    @DisplayName("페이 출금 예외 테스트")
    class withdrawExceptionTest {

        @Test
        public void 계좌가_없으면_예외를_던진다() {
            // given
            Long memberId = 1L;
            AccountTransactionRequestDto dto = new AccountTransactionRequestDto(10000L);
            given(accountService.getAccount(memberId)).willThrow(new IllegalArgumentException("잘못된 접근입니다."));

            // when & then
            assertThatThrownBy(() -> paymentService.withdraw(memberId, dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(walletService, never()).decrease(any(), any());
            verify(bankClient, never()).deposit(any(BankTransactionRequestDto.class));
        }

        @Test
        public void 잔액이_부족하면_예외를_던진다() {
            // given
            Long memberId = 1L;
            AccountTransactionRequestDto dto = new AccountTransactionRequestDto(10000L);
            Account account = Account.builder()
                    .walletId(1L)
                    .accountNumber("1234567890")
                    .build();
            given(accountService.getAccount(memberId)).willReturn(account);
            given(walletService.decrease(memberId, dto.amount())).willThrow(new IllegalArgumentException("잔액이 부족합니다."));

            // when & then
            assertThatThrownBy(() -> paymentService.withdraw(memberId, dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(bankClient, never()).deposit(any(BankTransactionRequestDto.class));
        }
    }

    @Nested
    @DisplayName("거래 내역 조회 예외 테스트")
    class getHistoryExceptionTest {

        @Test
        public void from이_to보다_늦으면_예외를_던진다() {
            // given
            Long memberId = 1L;
            LocalDate from = LocalDate.now();
            LocalDate to = LocalDate.now().minusMonths(1);
            Pageable pageable = PageRequest.of(0, 20);

            // when & then
            assertThatThrownBy(() -> paymentService.getHistory(memberId, from, to, pageable))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(paymentHistoryRepository, never()).findByWalletIdAndCreatedAtBetween(
                    any(), any(), any(), any()
            );
        }
    }
}