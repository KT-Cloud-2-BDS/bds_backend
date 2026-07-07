package com.bds.order.infrastructure.orderReward;

import com.bds.order.domain.orderReward.OrderRewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRewardPersistenceAdapter implements OrderRewardRepository {

    private final OrderRewardJpaRepository orderRewardJpaRepository;
    private final OrderRewardMapper orderRewardMapper;
}