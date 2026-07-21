package com.bds.payment.bank.application;

import com.bds.payment.bank.domain.bankTransaction.BankTransaction;
import com.bds.payment.bank.domain.bankTransaction.BankTransactionRepository;
import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCode;
import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCodeRepository;
import com.bds.payment.bank.presentation.request.BankAccountRequestDto;
import com.bds.payment.bank.presentation.request.BankTransactionRequestDto;
import com.bds.payment.bank.presentation.request.BankVerifyRequestDto;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankServiceUnitExceptionTest {

    @Mock private BankVerifyCodeRepository bankVerifyCodeRepository;
    @Mock private BankTransactionRepository bankTransactionRepository;

    @InjectMocks private BankService bankService;

    @Nested
    @DisplayName("계좌 인증코드 발송 예외 테스트")
    class sendVerificationCodeExceptionTest {

        @Test
        public void 이미_존재하는_계좌번호면_예외를_던진다() {
            // given
            BankAccountRequestDto dto = new BankAccountRequestDto("004", "1234567890", "홍길동");
            BankVerifyCode verifyCode = BankVerifyCode.create(dto, "1234");
            when(bankVerifyCodeRepository.findByAccountNumber(dto.accountNumber())).thenReturn(Optional.of(verifyCode));

            // when & then
            assertThrows(IllegalArgumentException.class,
                    () -> bankService.sendVerificationCode(dto));
            verify(bankVerifyCodeRepository, never()).save(any(BankVerifyCode.class));
        }
    }

    @Nested
    @DisplayName("계좌 인증코드 검증 예외 테스트")
    class verifyCodeExceptionTest {

        @Test
        public void 존재하지_않는_계좌번호면_예외를_던진다() {
            // given
            BankVerifyRequestDto dto = new BankVerifyRequestDto("1234567890", "1234");
            when(bankVerifyCodeRepository.findByAccountNumber(dto.accountNumber())).thenReturn(Optional.empty());

            // when & then
            assertThrows(IllegalArgumentException.class,
                    () -> bankService.verifyCode(dto));
        }
    }

    @Nested
    @DisplayName("입금 예외 테스트")
    class depositExceptionTest {

        @Test
        public void 중복된_거래번호면_예외를_던진다() {
            // given
            UUID tranSeqNo = UuidCreator.getTimeOrderedEpoch();
            BankTransactionRequestDto dto = new BankTransactionRequestDto("1234567890", 10000L, tranSeqNo);
            when(bankTransactionRepository.existsByTranSeqNo(tranSeqNo)).thenReturn(true);

            // when & then
            assertThrows(IllegalArgumentException.class,
                    () -> bankService.deposit(dto));
            verify(bankTransactionRepository, never()).save(any(BankTransaction.class));
        }
    }

    @Nested
    @DisplayName("출금 예외 테스트")
    class withdrawExceptionTest {

        @Test
        public void 중복된_거래번호면_예외를_던진다() {
            // given
            UUID tranSeqNo = UUID.randomUUID();
            BankTransactionRequestDto dto = new BankTransactionRequestDto("1234567890", 10000L, tranSeqNo);
            when(bankTransactionRepository.existsByTranSeqNo(tranSeqNo)).thenReturn(true);

            // when & then
            assertThrows(IllegalArgumentException.class,
                    () -> bankService.withdraw(dto));
            verify(bankTransactionRepository, never()).save(any(BankTransaction.class));
        }
    }
}