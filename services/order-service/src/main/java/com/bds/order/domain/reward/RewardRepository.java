package com.bds.order.domain.reward;

import java.util.List;

public interface RewardRepository {
    List<Reward> findAllByIdAndFundingId(List<Long> ids, Long fundingId);
}
