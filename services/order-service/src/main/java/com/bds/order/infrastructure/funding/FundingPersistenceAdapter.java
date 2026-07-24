package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.reward.BadgeType;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.presentation.dto.FundingCreateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FundingPersistenceAdapter implements FundingRepository {

    private final FundingJpaRepository fundingJpaRepository;
    private final FundingMapper fundingMapper;

    @Override
    public Optional<Funding> findById(Long id) {
        return fundingJpaRepository.findById(id)
                .map(fundingMapper::toDomain);
    }

    @Override
    public List<Funding> findAll() {
        return fundingJpaRepository.findAll().stream()
                .map(fundingMapper::toDomain)
                .toList();
    }


    @Override
    public List<Funding> findByStatus(FundingStatus status) {
        return fundingJpaRepository.findByStatus(status).stream()
                .map(fundingMapper::toDomain)
                .toList();
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
        if (funding.getId() != null) {
            FundingJpaEntity existing = fundingJpaRepository.findById(funding.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "[FundingPersistenceAdapter] Funding not found: fundingId=" + funding.getId()));
            existing.updateFrom(funding);
        } else {
            fundingJpaRepository.save(fundingMapper.toJpaEntity(funding));
        }
    }

    @Override
    public Funding saveWithRewards(Funding funding, List<FundingCreateRequestDto.RewardCreateDto> rewards) {
        FundingJpaEntity fundingEntity = fundingMapper.toJpaEntity(funding);

        List<RewardJpaEntity> rewardEntities = rewards.stream()
                .map(r -> new RewardJpaEntity(
                        null, fundingEntity, r.name(), r.description(),
                        r.limitQty(), r.limitQty(),
                        r.badgeType() != null ? BadgeType.valueOf(r.badgeType()) : null,
                        r.price(), r.offerAt(), r.shippingCharge()
                ))
                .toList();

        fundingEntity.addRewards(rewardEntities);
        FundingJpaEntity saved = fundingJpaRepository.save(fundingEntity);

        return fundingMapper.toDomain(saved);
    }

    @Override
    public List<Funding> findByStatusAndUpdatedAfter(FundingStatus status, LocalDateTime after) {
        return fundingJpaRepository.findByStatusAndUpdatedAfter(status, after)
                .stream()
                .map(fundingMapper::toDomain)
                .toList();
    }

    @Override
    public Page<Funding> findByTypeAndStatusIn(FundingType type, List<FundingStatus> statuses, Pageable pageable) {
        Page<FundingJpaEntity> entityPage = fundingJpaRepository.findByTypeAndStatusIn(type, statuses, pageable);
        return entityPage.map(fundingMapper::toDomain);
    }

}