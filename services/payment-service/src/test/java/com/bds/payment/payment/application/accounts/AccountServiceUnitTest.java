package com.bds.payment.payment.application.accounts;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.account.AccountRepository;
import com.bds.payment.payment.infrastructure.external.BankClient;
import com.bds.payment.payment.infrastructure.external.request.BankAccountRequestDto;
import com.bds.payment.payment.infrastructure.external.request.BankVerifyRequestDto;
import com.bds.payment.payment.infrastructure.external.response.BankAccountResponseDto;
import com.bds.payment.payment.presentation.request.AccountRegisterRequestDto;
import com.bds.payment.payment.presentation.request.AccountVerifyRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceUnitTest {

    @Mock private AccountRepository accountRepository;
    @Mock private WalletService walletService;
    @Mock private BankClient client;

    @InjectMocks private AccountService accountService;

    @Nested
    @DisplayName("계좌 등록 정상 테스트")
    class registerAccountTest {

        @Test
        public void 계좌가_없으면_정상적으로_등록한다() {
            // given
            Long memberId = 1L;
            Long walletId = 1L;
            AccountRegisterRequestDto dto = new AccountRegisterRequestDto("004", "1234567890", "홍길동");
            BankAccountResponseDto bankAccountResponseDto = new BankAccountResponseDto("1234567890", "홍길동");
            given(walletService.getWalletId(memberId)).willReturn(walletId);
            given(accountRepository.findById(walletId)).willReturn(Optional.empty());
            given(client.requestVerification(any(BankAccountRequestDto.class))).willReturn(bankAccountResponseDto);

            // when
            String result = accountService.registerAccount(memberId, dto);

            // then
            assertEquals("정상 처리되었습니다.", result);
            verify(accountRepository).save(any(Account.class));
            verify(client).requestVerification(any(BankAccountRequestDto.class));
        }

        @Test
        public void 미인증_계좌가_있으면_정보를_갱신하고_인증코드를_재발송한다() {
            Long memberId = 1L;
            Long walletId = 1L;
            AccountRegisterRequestDto dto =
                    new AccountRegisterRequestDto("088", "9999999999", "홍길동");

            Account account = Account.builder()
                    .walletId(walletId)
                    .bankCode("004")
                    .accountNumber("1234567890")
                    .holderName("홍길동")
                    .isVerified(false)
                    .build();

            given(walletService.getWalletId(memberId)).willReturn(walletId);
            given(accountRepository.findById(walletId)).willReturn(Optional.of(account));
            willAnswer(invocation -> invocation.getArgument(0))
                    .given(accountRepository)
                    .save(any(Account.class));
            given(client.requestVerification(any(BankAccountRequestDto.class)))
                    .willReturn(new BankAccountResponseDto("9999999999", "홍길동"));

            String result = accountService.registerAccount(memberId, dto);

            assertEquals("인증 요청을 재전송했습니다.", result);
            verify(accountRepository).save(account);
            verify(client).requestVerification(any(BankAccountRequestDto.class));
        }
    }

    @Nested
    @DisplayName("계좌 인증 정상 테스트")
    class verifyAccountTest {

        @Test
        public void 인증코드가_일치하면_정상처리한다() {
            // given
            Long memberId = 1L;
            Long walletId = 1L;
            AccountVerifyRequestDto dto = new AccountVerifyRequestDto("1234");
            Account account = Account.builder()
                    .walletId(walletId)
                    .accountNumber("1234567890")
                    .build();
            when(walletService.getWalletId(memberId)).thenReturn(walletId);
            when(accountRepository.findById(walletId)).thenReturn(Optional.of(account));
            when(client.confirmVerification(any(BankVerifyRequestDto.class))).thenReturn(true);

            // when
            String result = accountService.verifyAccount(memberId, dto);

            // then
            assertEquals("정상 처리되었습니다.", result);
            verify(client).confirmVerification(any(BankVerifyRequestDto.class));
            assertThat(account.getIsVerified()).isTrue();
        }
    }

    @Nested
    @DisplayName("계좌 조회 정상 테스트")
    class getAccountTest {

        @Test
        public void 계좌를_정상적으로_조회한다() {
            // given
            Long memberId = 1L;
            Long walletId = 1L;
            Account account = Account.builder()
                    .walletId(walletId)
                    .accountNumber("1234567890")
                    .build();
            when(walletService.getWalletId(memberId)).thenReturn(walletId);
            when(accountRepository.findById(walletId)).thenReturn(Optional.of(account));

            // when
            Account result = accountService.getAccount(memberId);

            // then
            assertNotNull(result);
            assertEquals("1234567890", result.getAccountNumber());
        }
    }
}