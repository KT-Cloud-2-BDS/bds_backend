package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletCreator {

    private final WalletRepository walletRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Wallet createWalletSafely(Long memberId) {
        try {
            log.info("지갑이 없어 자동 생성합니다. memberId={}", memberId);
            return walletRepository.save(Wallet.create(memberId));

        } catch (DataIntegrityViolationException e) {
            log.warn("동시 생성 감지, 재조회. memberId={}", memberId);
            return walletRepository.findByMemberId(memberId)
                    .orElseThrow(() -> new IllegalStateException(
                            "지갑 조회 실패. memberId=" + memberId));
        }
    }
}
