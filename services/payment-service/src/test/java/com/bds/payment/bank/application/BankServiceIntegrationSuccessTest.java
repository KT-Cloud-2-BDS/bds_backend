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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BankServiceIntegrationSuccessTest {

    @Autowired
    private BankService bankService;

    @Autowired
    private BankVerifyCodeRepository bankVerifyCodeRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Test
    void 계좌_인증_코드_발송이_정상_처리된다() {
        BankAccountRequestDto dto = new BankAccountRequestDto("004", "1234567890", "홍길동");

        BankAccountResponseDto result = bankService.sendVerificationCode(dto);

        assertThat(result).isNotNull();
        assertThat(bankVerifyCodeRepository.findByAccountNumber(dto.accountNumber())).isPresent();
    }

    @Test
    void 계좌_인증_코드_검증이_정상_처리된다() {
        BankVerifyRequestDto dto = new BankVerifyRequestDto("1234567890", "1234");
        BankVerifyCode verifyCode = BankVerifyCode.create(new BankAccountRequestDto("004", "1234567890", "홍길동"), "1234");
        bankVerifyCodeRepository.save(verifyCode);

        BankVerifyResponseDto result = bankService.verifyCode(dto);

        assertThat(result).isNotNull();
        assertThat(result.verified()).isTrue();
        assertThat(bankVerifyCodeRepository.findByAccountNumber(dto.accountNumber())).isEmpty();
    }

    @Test
    void 입금_거래가_정상_생성된다() {
        UUID tranSeqNo = UuidCreator.getTimeOrderedEpoch();
        BankTransactionRequestDto dto = new BankTransactionRequestDto("1234567890", 10000L, tranSeqNo);

        BankTransactionResponseDto result = bankService.deposit(dto);

        assertThat(result).isNotNull();
        assertThat(bankTransactionRepository.existsByTranSeqNo(tranSeqNo)).isTrue();
    }

    @Test
    void 출금_거래가_정상_생성된다() {
        UUID tranSeqNo = UuidCreator.getTimeOrderedEpoch();
        BankTransactionRequestDto dto = new BankTransactionRequestDto("1234567890", 10000L, tranSeqNo);

        BankTransactionResponseDto result = bankService.withdraw(dto);

        assertThat(result).isNotNull();
        assertThat(bankTransactionRepository.existsByTranSeqNo(tranSeqNo)).isTrue();
    }

    @Test
    void 거래_단건_조회가_정상_동작한다() {
        UUID tranSeqNo = UuidCreator.getTimeOrderedEpoch();
        BankTransaction transaction = BankTransaction.create(new BankTransactionRequestDto("1234567890", 10000L, tranSeqNo), TransactionType.DEPOSIT);
        bankTransactionRepository.save(transaction);

        BankTransactionUnitResponseDto result = bankService.getTransactionDetail(tranSeqNo);

        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
    }
}
