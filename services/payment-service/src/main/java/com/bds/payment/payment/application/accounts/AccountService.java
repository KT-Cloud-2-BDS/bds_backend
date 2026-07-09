package com.bds.payment.payment.application.accounts;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.account.AccountRepository;
import com.bds.payment.payment.infrastructure.external.BankClient;
import com.bds.payment.payment.infrastructure.external.request.BankAccountRequestDto;
import com.bds.payment.payment.infrastructure.external.request.BankVerifyRequestDto;
import com.bds.payment.payment.presentation.request.AccountRegisterRequestDto;
import com.bds.payment.payment.presentation.request.AccountVerifyRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final WalletService walletService;
    private final BankClient client;

    //TODO: 보안을 위한 값의 암호화가 진행되어야한다.

    public String registerAccount(Long memberId, AccountRegisterRequestDto dto) {
        Long walletId = walletService.getWalletId(memberId);

        Optional<Account> accountOptional = accountRepository.findById(walletId);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();

            if (account.getIsVerified()) {
                throw new IllegalArgumentException("이미 인증된 계좌가 등록되어 있습니다.");
            }

            account.updateAccount(dto);
            accountRepository.save(account);

            client.requestVerification(BankAccountRequestDto.to(dto));
            return "인증 요청을 재전송했습니다.";
        }

        accountRepository.save(Account.create(walletId, dto));
        client.requestVerification(BankAccountRequestDto.to(dto));

        return "정상 처리되었습니다.";
    }

    public String verifyAccount(Long memberId, AccountVerifyRequestDto dto) {
        Account account = getAccount(memberId);
        boolean isOk = client.confirmVerification(BankVerifyRequestDto.create(account.getAccountNumber(), dto));
        if (!isOk) throw new IllegalArgumentException("인증에 실패 했습니다.");
        account.markVerified();
        accountRepository.save(account);
        return "정상 처리되었습니다.";
    }

    public Account getAccount(Long memberId) {
        Long walletId = walletService.getWalletId(memberId);
        return accountRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 접근입니다."));
    }
}
