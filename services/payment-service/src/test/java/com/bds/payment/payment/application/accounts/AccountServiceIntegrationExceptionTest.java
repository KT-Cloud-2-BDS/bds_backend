package com.bds.payment.payment.application.accounts;

import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.account.AccountRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.infrastructure.external.BankClient;
import com.bds.payment.payment.presentation.request.AccountRegisterRequestDto;
import com.bds.payment.payment.presentation.request.AccountVerifyRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountServiceIntegrationExceptionTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private BankClient client;

    @Test
    void 미인증_계좌가_이미_있으면_계좌_정보를_갱신하고_외부_인증_요청을_다시_보낸다() {
        Long memberId = 1L;
        Long walletId = walletRepository.save(Wallet.create(memberId)).getId();

        Account existingAccount = Account.builder()
                .walletId(walletId)
                .bankCode("004")
                .accountNumber("1234567890")
                .holderName("홍길동")
                .isVerified(false)
                .build();
        accountRepository.save(existingAccount);

        AccountRegisterRequestDto dto =
                new AccountRegisterRequestDto("088", "9999999999", "홍길동");

        accountService.registerAccount(memberId, dto);

        verify(client).requestVerification(any());
    }

    @Test
    void 인증_완료_계좌가_이미_있으면_예외를_던지고_외부_인증_요청을_호출하지_않는다() {
        Long memberId = 1L;
        Long walletId = walletRepository.save(Wallet.create(memberId)).getId();

        Account existingAccount = Account.builder()
                .walletId(walletId)
                .bankCode("004")
                .accountNumber("1234567890")
                .holderName("홍길동")
                .isVerified(true)
                .build();

        accountRepository.save(existingAccount);

        AccountRegisterRequestDto dto =
                new AccountRegisterRequestDto("088", "9999999999", "홍길동");

        assertThatThrownBy(() -> accountService.registerAccount(memberId, dto))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_ALREADY_VERIFIED);
                    assertThat(ex.getMessage()).isEqualTo(ErrorCode.ACCOUNT_ALREADY_VERIFIED.getMessage());
                });
    }

    @Test
    void verifyAccount가_인증_실패_시_예외를_던진다() {
        Long memberId = 1L;
        Long walletId = walletRepository.save(Wallet.create(memberId)).getId();

        Account account = Account.builder()
                .walletId(walletId)
                .accountNumber("1234567890")
                .bankCode("004")
                .holderName("홍길동")
                .isVerified(false)
                .build();
        accountRepository.save(account);

        AccountVerifyRequestDto dto = new AccountVerifyRequestDto("9999");
        given(client.confirmVerification(any())).willReturn(false);

        assertThatThrownBy(() -> accountService.verifyAccount(memberId, dto))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_VERIFICATION_FAILED);
                    assertThat(ex.getMessage()).isEqualTo(ErrorCode.ACCOUNT_VERIFICATION_FAILED.getMessage());
                });
    }
}
