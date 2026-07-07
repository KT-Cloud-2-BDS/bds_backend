package com.bds.order.infrastructure.reward;

import com.bds.order.domain.reward.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RewardPersistenceAdaptor implements RewardRepository {

    private final RewardJpaRepository rewardJpaRepository;
    private final RewardMapper rewardMapper;
}
