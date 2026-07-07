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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BankService {
    //TODO: MVP 우선 적용으로 매직 넘버등 예외 케이스를 위한 보완은 추후 진행 예정
    private final BankVerifyCodeRepository bankVerifyCodeRepository;
    private final BankTransactionRepository bankTransactionRepository;

    private final Random random = new Random();

    public BankAccountResponseDto sendVerificationCode(BankAccountRequestDto dto) {
        Optional<BankVerifyCode> findBankVerifyCode = bankVerifyCodeRepository.findByAccountNumber(dto.accountNumber());
        // TODO: 재요청 시 코드 갱신 처리 필요
        if (findBankVerifyCode.isPresent()) throw new IllegalArgumentException("중복된 계좌번호입니다.");

        String verificationCode = getVerificationCode();
        return BankAccountResponseDto.from(bankVerifyCodeRepository.save(BankVerifyCode.create(dto, verificationCode)));
    }

    public BankVerifyResponseDto verifyCode(BankVerifyRequestDto dto) {
        BankVerifyCode bankVerifyCode = bankVerifyCodeRepository.findByAccountNumber(dto.accountNumber())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌번호입니다."));
        boolean verified = bankVerifyCode.getVerifyCode().equals(dto.code());
        // TODO: 인증코드 만료 처리 필요 (현재 만료 개념 없음)
        if (verified) bankVerifyCodeRepository.delete(bankVerifyCode);
        return new BankVerifyResponseDto(verified);
    }

    /**
     * Payment 기준 원화 환불
     */
    public BankTransactionResponseDto deposit(BankTransactionRequestDto dto) {
        validateDuplicated(dto.tranSeqNo());
        BankTransaction savedTransaction = bankTransactionRepository.save(BankTransaction.create(dto, TransactionType.DEPOSIT));
        return BankTransactionResponseDto.from(savedTransaction);
    }

    /**
     * Payment 기준 페이 충전
     */
    public BankTransactionResponseDto withdraw(BankTransactionRequestDto dto) {
        validateDuplicated(dto.tranSeqNo());
        BankTransaction savedTransaction = bankTransactionRepository.save(BankTransaction.create(dto, TransactionType.WITHDRAW));
        return BankTransactionResponseDto.from(savedTransaction);
    }

    public BankTransactionUnitResponseDto getTransactionDetail(UUID tranSeqNo) {
        return new BankTransactionUnitResponseDto(tranSeqNo, bankTransactionRepository.existsByTranSeqNo(tranSeqNo));
    }

    private String getVerificationCode() {
        int code = random.nextInt(9000) + 1000;
        return String.valueOf(code);
    }

    private void validateDuplicated(UUID tranSeqNo) {
        if (bankTransactionRepository.existsByTranSeqNo(tranSeqNo))
            throw new IllegalArgumentException("중복된 거래입니다.");
    }

}
