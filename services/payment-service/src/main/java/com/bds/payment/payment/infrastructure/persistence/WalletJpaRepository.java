package com.bds.payment.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, Long> {

    Optional<WalletJpaEntity> findByMemberId(Long memberId);
}
