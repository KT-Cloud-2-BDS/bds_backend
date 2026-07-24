package com.bds.order.domain.funding;

import com.bds.order.presentation.dto.FundingCreateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FundingRepository {
    Optional<Funding> findById(Long id);

    List<Funding> findAll();

    List<Funding> findByStatus(FundingStatus status);

    List<Funding> findFundingsReadyForJudgment(LocalDateTime now);

    List<Funding> findByStatusAndStartAtBeforeOrEqual(FundingStatus status, LocalDateTime now);

    List<Funding> findByStatusAndStartAtAfter(FundingStatus status, LocalDateTime now);

    List<Funding> findByStatusAndHoldToAfter(FundingStatus status, LocalDateTime now);

    Optional<Funding> findByIdForUpdate(Long fundingId);

    void save(Funding funding);

    Funding saveWithRewards(Funding funding, List<FundingCreateRequestDto.RewardCreateDto> rewards);

    List<Funding> findByStatusAndUpdatedAfter(FundingStatus status, LocalDateTime after);

    Page<Funding> findByTypeAndStatusIn(FundingType type, List<FundingStatus> statuses, Pageable pageable);
}
