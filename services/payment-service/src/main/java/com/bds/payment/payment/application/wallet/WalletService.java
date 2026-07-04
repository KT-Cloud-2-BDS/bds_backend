package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.presentation.response.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public WalletResponse getWallet(Long memberId) {
        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseGet(() -> createWalletSafely(memberId));
        return WalletResponse.from(wallet);
    }

    private Wallet createWalletSafely(Long memberId) {
        try {
            log.info("지갑이 없어 자동 생성합니다. memberId={}", memberId);
            return walletRepository.save(Wallet.create(memberId));
        } catch (DataIntegrityViolationException e) {
            log.warn("동시 생성 감지, 재조회. memberId={}", memberId);
            return walletRepository.findByMemberId(memberId)
                    .orElseThrow(() -> new IllegalStateException("지갑 생성 실패"));
        }
    }
}
