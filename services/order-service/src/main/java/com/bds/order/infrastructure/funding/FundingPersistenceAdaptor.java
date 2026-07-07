package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FundingPersistenceAdaptor implements FundingRepository {

    private final FundingJpaRepository fundingJpaRepository;
    private final FundingMapper fundingMapper;
}