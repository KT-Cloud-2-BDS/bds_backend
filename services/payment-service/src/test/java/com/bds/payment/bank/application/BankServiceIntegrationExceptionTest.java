package com.bds.payment.bank.application;

import com.bds.payment.bank.domain.bankTransaction.BankTransaction;
import com.bds.payment.bank.domain.bankTransaction.BankTransactionRepository;
import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCode;
import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCodeRepository;
import com.bds.payment.bank.domain.common.TransactionType;
import com.bds.payment.bank.presentation.request.BankAccountRequestDto;
import com.bds.payment.bank.presentation.request.BankTransactionRequestDto;
import com.bds.payment.bank.presentation.request.BankVerifyRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BankServiceIntegrationExceptionTest {

    @Autowired
    private BankService bankService;

    @Autowired
    private BankVerifyCodeRepository bankVerifyCodeRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Test
    void 중복된_계좌_번호로_인증코드_발송_시_예외가_발생한다() {
        BankAccountRequestDto dto = new BankAccountRequestDto("004", "1234567890", "홍길동");
        bankVerifyCodeRepository.save(BankVerifyCode.create(dto, "1234"));

        assertThatThrownBy(() -> bankService.sendVerificationCode(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 존재하지_않는_계좌_번호로_인증코드를_확인하면_예외가_발생한다() {
        BankVerifyRequestDto dto = new BankVerifyRequestDto("1234567890", "1234");

        assertThatThrownBy(() -> bankService.verifyCode(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 중복된_거래_번호로_입금_시_예외가_발생한다() {
        UUID tranSeqNo = UUID.randomUUID();
        BankTransactionRequestDto dto = new BankTransactionRequestDto("1234567890", 10000L, tranSeqNo);
        bankTransactionRepository.save(BankTransaction.create(dto, TransactionType.DEPOSIT));

        assertThatThrownBy(() -> bankService.deposit(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 중복된_거래_번호로_출금_시_예외가_발생한다() {
        UUID tranSeqNo = UUID.randomUUID();
        BankTransactionRequestDto dto = new BankTransactionRequestDto("1234567890", 10000L, tranSeqNo);
        bankTransactionRepository.save(BankTransaction.create(dto, TransactionType.WITHDRAW));

        assertThatThrownBy(() -> bankService.withdraw(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
