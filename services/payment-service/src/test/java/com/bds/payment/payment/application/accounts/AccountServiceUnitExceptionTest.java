package com.bds.payment.payment.application.accounts;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.account.AccountRepository;
import com.bds.payment.payment.infrastructure.external.BankClient;
import com.bds.payment.payment.infrastructure.external.request.BankVerifyRequestDto;
import com.bds.payment.payment.presentation.request.AccountVerifyRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceUnitExceptionTest {

    @Mock private AccountRepository accountRepository;
    @Mock private WalletService walletService;
    @Mock private BankClient client;

    @InjectMocks private AccountService accountService;

    @Nested
    @DisplayName("계좌 인증 예외 테스트")
    class verifyAccountExceptionTest {

        @Test
        public void 계좌가_없으면_예외를_던진다() {
            // given
            Long memberId = 1L;
            Long walletId = 1L;
            AccountVerifyRequestDto dto = new AccountVerifyRequestDto("1234");
            given(walletService.getWalletId(memberId)).willReturn(walletId);
            given(accountRepository.findById(walletId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accountService.verifyAccount(memberId, dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(client, never()).confirmVerification(any(BankVerifyRequestDto.class));
        }

        @Test
        public void 인증코드가_불일치하면_예외를_던진다() {
            // given
            Long memberId = 1L;
            Long walletId = 1L;
            AccountVerifyRequestDto dto = new AccountVerifyRequestDto("9999");
            Account account = Account.builder()
                    .walletId(walletId)
                    .accountNumber("1234567890")
                    .build();
            given(walletService.getWalletId(memberId)).willReturn(walletId);
            given(accountRepository.findById(walletId)).willReturn(Optional.of(account));
            given(client.confirmVerification(any(BankVerifyRequestDto.class))).willReturn(false);

            // when & then
            assertThatThrownBy(() -> accountService.verifyAccount(memberId, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("계좌 조회 예외 테스트")
    class getAccountExceptionTest {

        @Test
        public void 계좌가_없으면_예외를_던진다() {
            // given
            Long memberId = 1L;
            Long walletId = 1L;
            given(walletService.getWalletId(memberId)).willReturn(walletId);
            given(accountRepository.findById(walletId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accountService.getAccount(memberId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}