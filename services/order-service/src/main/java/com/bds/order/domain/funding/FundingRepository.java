package com.bds.order.domain.funding;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FundingRepository {
    Optional<Funding> findById(Long id);

    List<Funding> findFundingsReadyForJudgment(LocalDateTime now);

    List<Funding> findByStatusAndStartAtBeforeOrEqual(FundingStatus status, LocalDateTime now);

    List<Funding> findByStatusAndStartAtAfter(FundingStatus status, LocalDateTime now);

    List<Funding> findByStatusAndHoldToAfter(FundingStatus status, LocalDateTime now);

    Optional<Funding> findByIdForUpdate(Long fundingId);

    void save(Funding funding);
}
