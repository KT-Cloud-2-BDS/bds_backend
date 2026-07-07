package com.bds.order.domain.funding;

import java.util.Optional;

public interface FundingRepository {
    Optional<Funding> findById(Long id);
}
