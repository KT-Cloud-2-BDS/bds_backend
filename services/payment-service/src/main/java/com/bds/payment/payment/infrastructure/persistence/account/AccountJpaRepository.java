package com.bds.payment.payment.infrastructure.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {

}
