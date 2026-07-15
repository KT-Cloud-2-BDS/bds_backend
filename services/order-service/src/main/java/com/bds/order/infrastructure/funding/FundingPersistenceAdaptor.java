package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FundingPersistenceAdaptor implements FundingRepository {

    private final FundingJpaRepository fundingJpaRepository;
    private final FundingMapper fundingMapper;

    @Override
    public Optional<Funding> findById(Long id) {
        return fundingJpaRepository.findById(id)
                .map(fundingMapper::toDomain);
    }

    @Override
    public List<Funding> findFundingsReadyForJudgment(LocalDateTime now) {
        return fundingJpaRepository.findFundingsReadyForJudgment(now).stream()
                .map(fundingMapper::toDomain)
                .toList();
    }

    @Override
    public List<Funding> findByStatusAndStartAtBeforeOrEqual(FundingStatus status, LocalDateTime now) {
        return fundingJpaRepository.findByStatusAndStartAtBeforeOrEqual(status, now).stream()
                .map(fundingMapper::toDomain)
                .toList();
    }

    @Override
    public List<Funding> findByStatusAndStartAtAfter(FundingStatus status, LocalDateTime now) {
        return fundingJpaRepository.findByStatusAndStartAtAfter(status, now).stream()
                .map(fundingMapper::toDomain)
                .toList();
    }

    @Override
    public List<Funding> findByStatusAndHoldToAfter(FundingStatus status, LocalDateTime now) {
        return fundingJpaRepository.findByStatusAndHoldToAfter(status, now).stream()
                .map(fundingMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Funding> findByIdForUpdate(Long fundingId) {
        return fundingJpaRepository.findByIdForUpdate(fundingId)
                .map(fundingMapper::toDomain);
    }

    @Override
    public void save(Funding funding) {
        fundingJpaRepository.save(fundingMapper.toJpaEntity(funding));
    }
}