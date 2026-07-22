package com.bds.payment.payment.infrastructure.persistence.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, Long> {

    boolean existsByMemberId(Long memberId);

    Optional<WalletJpaEntity> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletJpaEntity w where w.memberId = :memberId")
    Optional<WalletJpaEntity> findByMemberIdWithLock(Long memberId);
}
