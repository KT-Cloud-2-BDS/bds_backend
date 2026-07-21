package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}