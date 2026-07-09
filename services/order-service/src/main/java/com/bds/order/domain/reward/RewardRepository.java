package com.bds.order.domain.reward;

import java.util.List;

public interface RewardRepository {
    List<Reward> findAllByIdAndFundingId(List<Long> ids, Long fundingId);

    void increaseRemainQty(Long rewardId, int qty);

    int decreaseStock(Long id, int qty);
}
