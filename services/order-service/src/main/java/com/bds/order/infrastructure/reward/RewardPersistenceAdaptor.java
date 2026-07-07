package com.bds.order.infrastructure.reward;

import com.bds.order.domain.reward.Reward;
import com.bds.order.domain.reward.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RewardPersistenceAdaptor implements RewardRepository {

    private final RewardJpaRepository rewardJpaRepository;
    private final RewardMapper rewardMapper;

    public List<Reward> findAllByIdAndFundingId(List<Long> ids, Long fundingId) {
        return rewardJpaRepository.findAllByIdAndFundingId(ids, fundingId).stream()
                .map(rewardMapper::toDomain)
                .toList();
    }

}
