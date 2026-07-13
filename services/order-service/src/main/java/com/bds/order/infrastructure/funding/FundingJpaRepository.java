package com.bds.order.infrastructure.funding;

import org.springframework.data.jpa.repository.JpaRepository;


public interface FundingJpaRepository extends JpaRepository<FundingJpaEntity, Long> {
}