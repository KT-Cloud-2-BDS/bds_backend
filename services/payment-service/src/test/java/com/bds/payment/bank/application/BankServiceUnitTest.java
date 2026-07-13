package com.bds.payment.bank.application;

import com.bds.payment.bank.domain.bankTransaction.BankTransaction;
import com.bds.payment.bank.domain.bankTransaction.BankTransactionRepository;
import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCode;
import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCodeRepository;
import com.bds.payment.bank.domain.common.TransactionType;
import com.bds.payment.bank.presentation.request.BankAccountRequestDto;
import com.bds.payment.bank.presentation.request.BankTransactionRequestDto;
import com.bds.payment.bank.presentation.request.BankVerifyRequestDto;
import com.bds.payment.bank.presentation.response.BankAccountResponseDto;
import com.bds.payment.bank.presentation.response.BankTransactionResponseDto;
import com.bds.payment.bank.presentation.response.BankTransactionUnitResponseDto;
import com.bds.payment.bank.presentation.response.BankVerifyResponseDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankServiceUnitTest {

    @Mock private BankVerifyCodeRepository bankVerifyCodeRepository;
    @Mock private BankTransactionRepository bankTransactionRepository;

    @InjectMocks private BankService bankService;

    @Nested
    @DisplayName("계좌 인증코드 발송 정상 테스트")
    class sendVerificationCodeTest {

        @Test
        public void 인증코드를_정상적으로_발송한다() {
            // given
            BankAccountRequestDto dto = new BankAccountRequestDto("004", "1234567890", "홍길동");
            BankVerifyCode verifyCode = BankVerifyCode.create(dto, "1234");
            when(bankVerifyCodeRepository.findByAccountNumber(dto.accountNumber())).thenReturn(Optional.empty());
            when(bankVerifyCodeRepository.save(any(BankVerifyCode.class))).thenReturn(verifyCode);

            // when
            BankAccountResponseDto result = bankService.sendVerificationCode(dto);

            // then
            assertNotNull(result);
            verify(bankVerifyCodeRepository).save(any(BankVerifyCode.class));
        }
    }

    @Nested
    @DisplayName("계좌 인증코드 검증 정상 테스트")
    class verifyCodeTest {

        @Test
        public void 인증코드가_일치하면_true를_반환한다() {
            // given
            BankVerifyRequestDto dto = new BankVerifyRequestDto("1234567890", "1234");
            BankVerifyCode verifyCode = BankVerifyCode.create(new BankAccountRequestDto("004", "1234567890", "홍길동"), "1234");
            when(bankVerifyCodeRepository.findByAccountNumber(dto.accountNumber())).thenReturn(Optional.of(verifyCode));

            // when
            BankVerifyResponseDto result = bankService.verifyCode(dto);

            // then
            assertTrue(result.verified());
            verify(bankVerifyCodeRepository).delete(verifyCode);
        }

        @Test
        public void 인증코드가_불일치하면_false를_반환한다() {
            // given
            BankVerifyRequestDto dto = new BankVerifyRequestDto("1234567890", "9999");
            BankVerifyCode verifyCode = BankVerifyCode.create(new BankAccountRequestDto("004", "1234567890", "홍길동"), "1234");
            when(bankVerifyCodeRepository.findByAccountNumber(dto.accountNumber())).thenReturn(Optional.of(verifyCode));

            // when
            BankVerifyResponseDto result = bankService.verifyCode(dto);

            // then
            assertFalse(result.verified());
            verify(bankVerifyCodeRepository, never()).delete(verifyCode);
        }
    }

    @Nested
    @DisplayName("입금 정상 테스트")
    class depositTest {

        @Test
        public void 입금을_정상적으로_처리한다() {
            // given
            UUID tranSeqNo = UuidCreator.getTimeOrderedEpoch();
            BankTransactionRequestDto dto = new BankTransactionRequestDto("1234567890", 10000L, tranSeqNo);
            BankTransaction transaction = BankTransaction.create(dto, TransactionType.DEPOSIT);
            when(bankTransactionRepository.existsByTranSeqNo(tranSeqNo)).thenReturn(false);
            when(bankTransactionRepository.save(any(BankTransaction.class))).thenReturn(transaction);

            // when
            BankTransactionResponseDto result = bankService.deposit(dto);

            // then
            assertNotNull(result);
            verify(bankTransactionRepository).save(any(BankTransaction.class));
        }
    }

    @Nested
    @DisplayName("출금 정상 테스트")
    class withdrawTest {

        @Test
        public void 출금을_정상적으로_처리한다() {
            // given
            UUID tranSeqNo = UUID.randomUUID();
            BankTransactionRequestDto dto = new BankTransactionRequestDto("1234567890", 10000L, tranSeqNo);
            BankTransaction transaction = BankTransaction.create(dto, TransactionType.WITHDRAW);
            when(bankTransactionRepository.existsByTranSeqNo(tranSeqNo)).thenReturn(false);
            when(bankTransactionRepository.save(any(BankTransaction.class))).thenReturn(transaction);

            // when
            BankTransactionResponseDto result = bankService.withdraw(dto);

            // then
            assertNotNull(result);
            verify(bankTransactionRepository).save(any(BankTransaction.class));
        }
    }

    @Nested
    @DisplayName("거래 단건 조회 정상 테스트")
    class getTransactionDetailTest {

        @Test
        public void 거래가_존재하면_true를_반환한다() {
            // given
            UUID tranSeqNo = UUID.randomUUID();
            when(bankTransactionRepository.existsByTranSeqNo(tranSeqNo)).thenReturn(true);

            // when
            BankTransactionUnitResponseDto result = bankService.getTransactionDetail(tranSeqNo);

            // then
            assertNotNull(result);
            assertTrue(result.exists());
        }
    }
}