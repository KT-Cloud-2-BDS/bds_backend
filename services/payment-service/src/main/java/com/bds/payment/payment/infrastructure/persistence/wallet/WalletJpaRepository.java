package com.bds.payment.payment.infrastructure.persistence.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, Long> {

    boolean existsByMemberId(Long memberId);

    Optional<WalletJpaEntity> findByMemberId(Long memberId);
}
